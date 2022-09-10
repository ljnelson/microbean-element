public record Record(String foo) {
  @Override
    public final String foo() {
    return this.foo;
  }
}
