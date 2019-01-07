package org.broadinstitute.sting.utils.exceptions;

import java.lang.reflect.InvocationTargetException;

public class DynamicClassResolutionException extends UserException {
   public DynamicClassResolutionException(Class c, Exception ex) {
      super(String.format("Could not create module %s because %s caused by exception %s", c.getSimpleName(), moreInfo(ex), ex.getMessage()));
   }

   private static String moreInfo(Exception ex) {
      try {
         throw ex;
      } catch (InstantiationException var2) {
         return "BUG: cannot instantiate class: must be concrete class";
      } catch (NoSuchMethodException var3) {
         return "BUG: Cannot find expected constructor for class";
      } catch (IllegalAccessException var4) {
         return "Cannot instantiate class (Illegal Access)";
      } catch (InvocationTargetException var5) {
         return "Cannot instantiate class (Invocation failure)";
      } catch (Exception var6) {
         return String.format("an exception of type %s occurred", var6.getClass().getSimpleName());
      }
   }
}
