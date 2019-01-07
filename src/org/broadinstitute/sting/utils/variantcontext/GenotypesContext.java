package org.broadinstitute.sting.utils.variantcontext;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class GenotypesContext implements List<Genotype> {
   public static final GenotypesContext NO_GENOTYPES = (new GenotypesContext(new ArrayList(0), new HashMap(0), Collections.emptyList())).immutable();
   List sampleNamesInOrder;
   Map sampleNameToOffset;
   ArrayList notToBeDirectlyAccessedGenotypes;
   boolean immutable;

   protected GenotypesContext() {
      this(10);
   }

   @Requires({"n >= 0"})
   protected GenotypesContext(int n) {
      this(new ArrayList(n));
   }

   @Requires({"genotypes != null", "noDups(genotypes)"})
   protected GenotypesContext(ArrayList genotypes) {
      this.sampleNamesInOrder = null;
      this.sampleNameToOffset = null;
      this.immutable = false;
      this.notToBeDirectlyAccessedGenotypes = genotypes;
      this.sampleNameToOffset = null;
   }

   @Requires({"genotypes != null", "noDups(genotypes)", "sampleNameToOffset != null", "sampleNamesInOrder != null", "genotypes.size() == sampleNameToOffset.size()", "genotypes.size() == sampleNamesInOrder.size()"})
   protected GenotypesContext(ArrayList genotypes, Map sampleNameToOffset, List sampleNamesInOrder) {
      this.sampleNamesInOrder = null;
      this.sampleNameToOffset = null;
      this.immutable = false;
      this.notToBeDirectlyAccessedGenotypes = genotypes;
      this.sampleNameToOffset = sampleNameToOffset;
      this.sampleNamesInOrder = sampleNamesInOrder;
   }

   @Ensures({"result != null"})
   public static final GenotypesContext create() {
      return new GenotypesContext();
   }

   @Requires({"nGenotypes >= 0"})
   @Ensures({"result != null"})
   public static final GenotypesContext create(int nGenotypes) {
      return new GenotypesContext(nGenotypes);
   }

   @Requires({"genotypes != null", "sampleNameToOffset != null", "sampleNamesInOrder != null", "sameSamples(genotypes, sampleNamesInOrder)", "sameSamples(genotypes, sampleNameToOffset.keySet())"})
   @Ensures({"result != null"})
   public static final GenotypesContext create(ArrayList genotypes, Map sampleNameToOffset, List sampleNamesInOrder) {
      return new GenotypesContext(genotypes, sampleNameToOffset, sampleNamesInOrder);
   }

   @Requires({"genotypes != null"})
   @Ensures({"result != null"})
   public static final GenotypesContext create(ArrayList genotypes) {
      return genotypes == null ? NO_GENOTYPES : new GenotypesContext(genotypes);
   }

   @Requires({"genotypes != null"})
   @Ensures({"result != null"})
   public static final GenotypesContext create(Genotype... genotypes) {
      return create(new ArrayList(Arrays.asList(genotypes)));
   }

   @Requires({"toCopy != null"})
   @Ensures({"result != null"})
   public static final GenotypesContext copy(GenotypesContext toCopy) {
      return create(new ArrayList(toCopy.getGenotypes()));
   }

   @Ensures({"result != null"})
   public static final GenotypesContext copy(Collection toCopy) {
      return toCopy == null ? NO_GENOTYPES : create(new ArrayList(toCopy));
   }

   public final GenotypesContext immutable() {
      this.immutable = true;
      return this;
   }

   public boolean isMutable() {
      return !this.immutable;
   }

   public final void checkImmutability() {
      if (this.immutable) {
         throw new IllegalAccessError("GenotypeMap is currently immutable, but a mutator method was invoked on it");
      }
   }

   @Ensures({"sampleNameToOffset == null"})
   protected void invalidateSampleNameMap() {
      this.sampleNameToOffset = null;
   }

   @Ensures({"sampleNamesInOrder == null"})
   protected void invalidateSampleOrdering() {
      this.sampleNamesInOrder = null;
   }

   @Ensures({"sampleNamesInOrder != null", "sameSamples(notToBeDirectlyAccessedGenotypes, sampleNamesInOrder)"})
   protected void ensureSampleOrdering() {
      if (this.sampleNamesInOrder == null) {
         this.sampleNamesInOrder = new ArrayList(this.size());

         for(int i = 0; i < this.size(); ++i) {
            this.sampleNamesInOrder.add(((Genotype)this.getGenotypes().get(i)).getSampleName());
         }

         Collections.sort(this.sampleNamesInOrder);
      }

   }

   @Ensures({"sampleNameToOffset != null", "sameSamples(notToBeDirectlyAccessedGenotypes, sampleNameToOffset.keySet())"})
   protected void ensureSampleNameMap() {
      if (this.sampleNameToOffset == null) {
         this.sampleNameToOffset = new HashMap(this.size());

         for(int i = 0; i < this.size(); ++i) {
            this.sampleNameToOffset.put(((Genotype)this.getGenotypes().get(i)).getSampleName(), i);
         }
      }

   }

   protected void ensureAll() {
      this.ensureSampleNameMap();
      this.ensureSampleOrdering();
   }

   protected ArrayList getGenotypes() {
      return this.notToBeDirectlyAccessedGenotypes;
   }

   public void clear() {
      this.checkImmutability();
      this.invalidateSampleNameMap();
      this.invalidateSampleOrdering();
      this.getGenotypes().clear();
   }

   public int size() {
      return this.getGenotypes().size();
   }

   public boolean isEmpty() {
      return this.getGenotypes().isEmpty();
   }

   @Requires({"genotype != null", "get(genotype.getSampleName()) == null"})
   @Ensures({"noDups(getGenotypes())"})
   public boolean add(Genotype genotype) {
      this.checkImmutability();
      this.invalidateSampleOrdering();
      if (this.sampleNameToOffset != null) {
         this.sampleNameToOffset.put(genotype.getSampleName(), this.size());
      }

      return this.getGenotypes().add(genotype);
   }

   @Requires({"! contains(genotype)"})
   @Ensures({"noDups(getGenotypes())"})
   public void add(int i, Genotype genotype) {
      throw new UnsupportedOperationException();
   }

   @Requires({"! containsAny(genotypes)"})
   @Ensures({"noDups(getGenotypes())"})
   public boolean addAll(Collection genotypes) {
      this.checkImmutability();
      this.invalidateSampleOrdering();
      if (this.sampleNameToOffset != null) {
         int pos = this.size();
         Iterator j = genotypes.iterator();

         while(j.hasNext()) {
            Genotype g = (Genotype)j.next();
            this.sampleNameToOffset.put(g.getSampleName(), pos++);
         }
      }

      return this.getGenotypes().addAll(genotypes);
   }

   public boolean addAll(int i, Collection genotypes) {
      throw new UnsupportedOperationException();
   }

   public boolean contains(Object o) {
      return this.getGenotypes().contains(o);
   }

   public boolean containsAll(Collection objects) {
      return this.getGenotypes().containsAll(objects);
   }

   private boolean containsAny(Collection genotypes) {
      Iterator j = genotypes.iterator();

      Genotype g;
      do {
         if (!j.hasNext()) {
            return false;
         }

         g = (Genotype)j.next();
      } while(!this.contains(g));

      return true;
   }

   public Genotype get(int i) {
      return (Genotype)this.getGenotypes().get(i);
   }

   public Genotype get(String sampleName) {
      Integer offset = this.getSampleI(sampleName);
      return offset == null ? null : (Genotype)this.getGenotypes().get(offset);
   }

   private Integer getSampleI(String sampleName) {
      this.ensureSampleNameMap();
      return (Integer)this.sampleNameToOffset.get(sampleName);
   }

   public int indexOf(Object o) {
      return this.getGenotypes().indexOf(o);
   }

   public Iterator iterator() {
      return this.getGenotypes().iterator();
   }

   public int lastIndexOf(Object o) {
      return this.getGenotypes().lastIndexOf(o);
   }

   public ListIterator listIterator() {
      throw new UnsupportedOperationException();
   }

   public ListIterator listIterator(int i) {
      throw new UnsupportedOperationException();
   }

   public Genotype remove(int i) {
      this.checkImmutability();
      this.invalidateSampleNameMap();
      this.invalidateSampleOrdering();
      return (Genotype)this.getGenotypes().remove(i);
   }

   public boolean remove(Object o) {
      this.checkImmutability();
      this.invalidateSampleNameMap();
      this.invalidateSampleOrdering();
      return this.getGenotypes().remove(o);
   }

   public boolean removeAll(Collection objects) {
      this.checkImmutability();
      this.invalidateSampleNameMap();
      this.invalidateSampleOrdering();
      return this.getGenotypes().removeAll(objects);
   }

   public boolean retainAll(Collection objects) {
      this.checkImmutability();
      this.invalidateSampleNameMap();
      this.invalidateSampleOrdering();
      return this.getGenotypes().retainAll(objects);
   }

   @Ensures({"noDups(getGenotypes())"})
   public Genotype set(int i, Genotype genotype) {
      this.checkImmutability();
      Genotype prev = (Genotype)this.getGenotypes().set(i, genotype);
      this.invalidateSampleOrdering();
      if (this.sampleNameToOffset != null) {
         this.sampleNameToOffset.remove(prev.getSampleName());
         this.sampleNameToOffset.put(genotype.getSampleName(), i);
      }

      return prev;
   }

   @Requires({"genotype != null"})
   public Genotype replace(Genotype genotype) {
      this.checkImmutability();
      Integer offset = this.getSampleI(genotype.getSampleName());
      return offset == null ? null : this.set(offset, genotype);
   }

   public List subList(int i, int i1) {
      return this.getGenotypes().subList(i, i1);
   }

   public Object[] toArray() {
      return this.getGenotypes().toArray();
   }

   public Object[] toArray(Object[] ts) {
      return this.getGenotypes().toArray(ts);
   }

   @Requires({"sampleNamesInOrder != null"})
   public Iterable iterateInSampleNameOrder(final Iterable sampleNamesInOrder) {
      return new Iterable() {
         public Iterator iterator() {
            return GenotypesContext.this.new InOrderIterator(sampleNamesInOrder.iterator());
         }
      };
   }

   public Iterable iterateInSampleNameOrder() {
      return this.iterateInSampleNameOrder(this.getSampleNamesOrderedByName());
   }

   @Ensures({"result != null"})
   public Set getSampleNames() {
      this.ensureSampleNameMap();
      return this.sampleNameToOffset.keySet();
   }

   @Ensures({"result != null"})
   public List getSampleNamesOrderedByName() {
      this.ensureSampleOrdering();
      return this.sampleNamesInOrder;
   }

   @Requires({"sample != null"})
   public boolean containsSample(String sample) {
      this.ensureSampleNameMap();
      return this.sampleNameToOffset.containsKey(sample);
   }

   @Requires({"samples != null"})
   public boolean containsSamples(Collection samples) {
      return this.getSampleNames().containsAll(samples);
   }

   @Requires({"samples != null"})
   @Ensures({"result != null"})
   public GenotypesContext subsetToSamples(Set samples) {
      int nSamples = samples.size();
      if (nSamples == 0) {
         return NO_GENOTYPES;
      } else {
         GenotypesContext subset = create(samples.size());
         Iterator j = samples.iterator();

         while(j.hasNext()) {
            String sample = (String)j.next();
            Genotype g = this.get(sample);
            if (g != null) {
               subset.add(g);
            }
         }

         return subset;
      }
   }

   public String toString() {
      List gS = new ArrayList();
      Iterator j = this.iterateInSampleNameOrder().iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();
         gS.add(g.toString());
      }

      return "[" + join(",", gS) + "]";
   }

   private static String join(String separator, Collection objects) {
      if (objects.isEmpty()) {
         return "";
      } else {
         Iterator iter = objects.iterator();
         Object first = iter.next();
         if (!iter.hasNext()) {
            return first.toString();
         } else {
            StringBuilder ret = new StringBuilder(first.toString());

            while(iter.hasNext()) {
               ret.append(separator);
               ret.append(iter.next().toString());
            }

            return ret.toString();
         }
      }
   }

   protected static final boolean noDups(Collection genotypes) {
      Set names = new HashSet(genotypes.size());
      Iterator j = genotypes.iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();
         if (names.contains(g.getSampleName())) {
            return false;
         }

         names.add(g.getSampleName());
      }

      return true;
   }

   protected static final boolean sameSamples(List genotypes, Collection sampleNamesInOrder) {
      Set names = new HashSet(sampleNamesInOrder);
      if (names.size() != sampleNamesInOrder.size()) {
         return false;
      } else if (genotypes.size() != names.size()) {
         return false;
      } else {
         Iterator j = genotypes.iterator();

         Genotype g;
         do {
            if (!j.hasNext()) {
               return true;
            }

            g = (Genotype)j.next();
         } while(names.contains(g.getSampleName()));

         return false;
      }
   }

   private final class InOrderIterator implements Iterator {
      final Iterator sampleNamesInOrder;

      private InOrderIterator(Iterator sampleNamesInOrder) {
         this.sampleNamesInOrder = sampleNamesInOrder;
      }

      public boolean hasNext() {
         return this.sampleNamesInOrder.hasNext();
      }

      public Genotype next() {
         return GenotypesContext.this.get((String)this.sampleNamesInOrder.next());
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      // $FF: synthetic method
      InOrderIterator(Iterator x1, Object x2) {
         this(x1);
      }
   }
}
