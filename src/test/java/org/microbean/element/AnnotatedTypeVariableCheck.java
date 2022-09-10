class AnnotatedTypeVariableCheck {

  private static String[] strings() {
    return new String[0];
  }

  private static <@Gorp T> T frob() {
    return null;
  }

  @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
  private static @interface Gorp {}

}
