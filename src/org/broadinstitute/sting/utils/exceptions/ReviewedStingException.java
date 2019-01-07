package org.broadinstitute.sting.utils.exceptions;

public class ReviewedStingException extends StingException {
   public ReviewedStingException(String msg) {
      super(msg);
   }

   public ReviewedStingException(String message, Throwable throwable) {
      super(message, throwable);
   }
}
