package org.broadinstitute.sting.utils.help;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DocumentedGATKFeature {
   boolean enable() default true;

   String groupName();

   String summary() default "";

   Class[] extraDocs() default {};
}
