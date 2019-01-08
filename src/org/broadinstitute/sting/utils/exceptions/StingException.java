package org.broadinstitute.sting.utils.exceptions;

public class StingException extends RuntimeException {
   public StingException(String msg) {
      super(msg);
   }

   public StingException(String message, Throwable throwable) {
      super(message, throwable);
   }
}
