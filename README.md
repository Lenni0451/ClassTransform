# ClassTransform
A lightweight injection library using ASM. It abstracts away the complexity of ASM and makes it more user-friendly.\
ClassTransform is inspired by the [SpongePowered/Mixin](https://github.com/SpongePowered/Mixin) library.
If you have used Mixins before, you will feel right at home with ClassTransform.

Because ClassTransform offers similar functionality to Mixins, you can use the [MixinsTranslator](https://github.com/Lenni0451/ClassTransform/wiki/Submodules#mixinstranslator) to use Mixins code without any changes.\
It also supports some annotations from the popular [LlamaLad7/MixinExtras](https://github.com/LlamaLad7/MixinExtras) extension library.

ClassTransform was mainly designed for use with Java agents which is the recommended and most stable way to use it.\
However, it can also be used manually by invoking the `TransformerManager` directly or by using the `InjectionClassLoader`.

## Usage
For usage examples and a detailed explanation of the library, please check out the [wiki](https://github.com/Lenni0451/ClassTransform/wiki). All commonly used classes and methods are documented there and in the Javadocs.

If you want to quickly get started and figure the rest out yourself, check out the [Quick Start](#quick-start) section.

### Gradle/Maven
To use ClassTransform with Gradle/Maven you can get it from [maven central](https://mvnrepository.com/artifact/net.lenni0451.classtransform).\
You can also find instructions on how to implement it into your build script there.

<details>
<summary>Gradle</summary>

You need to replace ``x.x.x` with the latest version number.\
You can find it on the maven central page or in the GitHub releases.
```groovy
repositories {
    mavenCentral()
}

dependencies {
    include "net.lenni0451.classtransform:core:x.x.x"
}
```

</details>

<details>
<summary>Maven</summary>

You need to replace ``x.x.x` with the latest version number.\
You can find it on the maven central page or in the GitHub releases.
```xml
<dependency>
  <groupId>net.lenni0451.classtransform</groupId>
  <artifactId>core</artifactId>
  <version>x.x.x</version>
</dependency>
```

</details>

### Jar File
If you just want the latest jar file you can download it from my [Jenkins](https://build.lenni0451.net/job/ClassTransform/).

## Quick Start
This is a simple example of how to use ClassTransform together with a Java agent.

Creating a `TransformerManager`, adding a transformer and hooking it into the instrumentation.
```java
public static void agentmain(final String args, final Instrumentation instrumentation) {
    TransformerManager transformerManager = new TransformerManager(new BasicClassProvider());
    transformerManager.addTransformer("org.example.MyTransformer");
    transformerManager.hookInstrumentation(instrumentation);
}
```

A simple transformer could look like this:
```java
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;

@CTransformer(OtherClass.class)
public class MyTransformer {

    @CInject(method = "<init>", target = @CTarget("RETURN"))
    public void inject() {
        System.out.println("Hello from the constructor!");
    }

}
```
This transformer injects into the constructor of `OtherClass` and prints a message.
