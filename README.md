# Kson

Kson several utilities to JSON serialization in Kotlin:
- KSONUtil object contains standard methods to serialize/deserialize objects.
- Configuration class can be used convenient storage of all serializable objects and itself.
    - Use 'save(path: File)' method to save the configuration and 'load(path: File)' to load it.
- StaticConfiguration class can be implemented by Kotlin objects to make them easily serializable.
    - Use 'save(path: File)' method to save the object and 'load(path: File)' to load it. Use them directly from object.
    - It supports nested objects as well.
- Property class is an internal class with purpose of maintaining number types after they've been serialized.

WARNING: Due to either my incompetence or more likely a bug in JVM, modification of final fields in Kotlin objects if said
objects contain private fields doesn't work. Changing modifier field flags doesn't work. Only one way serialization works.
So... Instead of choosing, don't use private fields in serialized objects. StaticConfiguration should work fine otherwise.
