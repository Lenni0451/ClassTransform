# ClassTransform
A lightweight, mixin like injection lib using ASM.  
The usage is like Mixins. You can almost copy-paste mixins code and it works.

## Why?
I wanted a lightweight version of mixins which I can easily add into any program.  
It even contains a custom ClassLoader to inject into classes before loading them if you can't resort to agents.

## Usage
### Gradle/Maven/Jar download
To use ClassTransform with Gradle/Maven you can check out the repo on [Jitpack](https://jitpack.io/#Lenni0451/ClassTransform).  
If you want a jar file, you can also download it from Jitpack:  
``
https://jitpack.io/com/github/Lenni0451/ClassTransform/<version>/ClassTransform-<version>.jar
``  
As an example:
For version ``cf1f3f88ea`` this results in the following link:  
``
https://jitpack.io/com/github/Lenni0451/ClassTransform/cf1f3f88ea/ClassTransform-cf1f3f88ea.jar
``

### Transformer Manager
The ``TransformerManager`` is the main class which handles the entire injection process.  
When creating a new ``TransformerManager`` you have to provide a ``IClassProvider`` and optionally a ``AMapper``.  
The ``IClassProvider`` is used to get the bytecode of classes if needed for e.g. frame computation.  
The ``AMapper`` can provide mappings which get automatically applied to the transformers to allow injection obfuscated code.  

### Transformer types
There are different types of transformers available:

| Transformer Type     | Description                                                                                                       |
|----------------------|-------------------------------------------------------------------------------------------------------------------|
| IBytecodeTransformer | The IBytecodeTransformer can modify the bytecode of every class before applying any other transformer             |
| IRawTransformer      | The IRawTransformer gets access to the ClassNode before the default transformers are applied                      |
| Default Transformer  | The default transformers are the ones you know from Mixins                                                        |
| IPostTransformer     | The IPostTransformer gets the modified output bytecode. Mainly used for dumping classes if something doesn't work |

### Annotations
The following injection annotations are available:

| Annotation Type                     | Description                                                                                             |
|-------------------------------------|---------------------------------------------------------------------------------------------------------|
| [CASM](#CASM)                       | The access to the ClassNode of the entire class or a MethodNode of the wanted method                    |
| [CInject](#CInject)                 | Inject into any method at the given targets                                                             |
| [CModifyConstant](#CModifyConstant) | Modify a constant value in a method (null/int/long/float/double/string)                                 |
| [COverride](#COverride)             | Override any method in the target class                                                                 |
| [CRedirect](#CRedirect)             | Redirect a method call, a field get/put or new object to your injection method                          |
| [CWrapCatch](#CWrapCatch)           | Wrap a try-catch block around the entire method or a single invoke instruction and handle the exception |

The following util annotations are available:

| Annotation Type | Description                                                                      |
|-----------------|----------------------------------------------------------------------------------|
| CShadow         | Create a shadow of a field to access it in the transformer class                 |
| CSlice          | Choose any target in a method to create a slice to allow more precise injections |
| CTarget         | Choose the target to inject to                                                   |
| CTransformer    | Mark a class as a transformer and define the injected classes                    |

### Injecting using an Agent
Using an Agent is the preferred way to inject code using ClassTransform.
You can easily hook the Instrumentation by calling the ``hookInstrumentation`` method.
Example:
````java
public static void agentmain(String args, Instrumentation instrumentation) throws Throwable {
    TransformerManager transformerManager = new TransformerManager(new BasicClassProvider());
    transformerManager.addTransformer("net.lenni0451.classtransform.TestTransformer");
    transformerManager.hookInstrumentation(instrumentation);
}
````
The class ``net.lenni0451.classtransform.TestTransformer`` in this example is our default transformer.

### Injecting using a ClassLoader
ClassTransform also provides the ``InjectionClassLoader`` to allow injection into classes without transformation access.  
When creating a new ``InjectionClassLoader`` you have to provide a ``TransformerManager`` and an ``URL[]`` array with the classpath.  
Optionally a parent ClassLoader can be provided.  
Example:
````java
public static void main(String[] args) throws Throwable {
    TransformerManager transformerManager = new TransformerManager(new BasicClassProvider());
    transformerManager.addTransformer("net.lenni0451.classtransform.TestTransformer");
    InjectionClassLoader classLoader = new InjectionClassLoader(transformerManager, Launcher.class.getProtectionDomain().getCodeSource().getLocation());
    classLoader.executeMain("net.lenni0451.classtransform.Main", "main", args);
}
````

You can add resources to the ClassLoader by using the ``addRuntimeResource`` method.  
Example:
````java
classLoader.addRuntimeResource("test", new byte[123]);
````
This also works with (runtime generated) classes.

### Default Transformer
The default transformers are annotated with ``@CTransformer``.  
As the arguments of the annotation you can pass a class array and/or a String array with class names.  
Example:
````java
@CTransformer(Example.class)
public class TestTransformer {
````

### Annotation Examples
#### CASM
With the ``CASM`` annotation you can access the ClassNode of the entire class or a MethodNode of the wanted method.  
Example:
````java
@CASM
public void test(ClassNode classNode) {
    System.out.println(classNode.name);
}

@CASM("toString")
public void test(MethodNode methodNode) {
    System.out.println(methodNode.name);
}
````

#### CInject
With the ``CInject`` annotation you can inject into any method at the given targets.  
Example:
````java
@CInject(method = "equals(Ljava/lang/Object;)Z",
        target = @CTarget(
                value = "THROW",
                shift = CTarget.Shift.BEFORE,
                ordinal = 1
        ),
        slice = @CSlice(
                from = @CTarget(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z")
        ),
        cancellable = true)
public void test(Object other, InjectionCallback callback) {
    System.out.println("Checking equals: " + other);
}
````
This example method injects into the ``equals`` method of the given class.  
It injects a method call above the second ``throw`` instruction after the first ``Objects#equals`` call.  
The called method is your injection method, and you can cancel the rest of the original method by using the ``InjectionCallback``.  
In this case the ``equals`` method must return a boolean. So you have to call ``InjectionCallback#setReturnValue`` with a boolean.  
If your inject target is ``RETURN/TAIL/THROW`` you can use ``InjectionCallback#getReturnValue`` to get the current return value/thrown exception.

#### CModifyConstant
With the ``CModifyConstant`` annotation you can modify a constant value in a method (null/int/long/float/double/string).  
Example:
````java
@CModifyConstant(
        method = "log",
        stringValue = "[INFO]")
public String infoToFatal(String originalConstant) {
    return "[FATAL]";
}
````
This example method modifies the string constant value of the ``log`` method.  
The method is called with the original constant value as argument and must return the new constant value.  
Only one constant value can be modified at a time.

#### COverride
With the ``COverride`` annotation you can override any method in the target class.  
Example:
````java
@COverride
public String toString() {
    return "Test";
}
````
This example method overrides the ``toString`` method of the given class.  
The arguments and return type of the overridden method need to be the same.  
You can also set the target method name as a parameter in the annotation.

#### CRedirect
With the ``CRedirect`` annotation you can redirect a method call, a field get/put or new object to your injection method.
Example:
````java
@CRedirect(
        method = "getNewRandom",
        target = @CTarget(
                value = "NEW",
                target = "java/util/Random"
        ),
        slice = @CSlice(
                to = @CTarget("RETURN")
        ))
public Random makeSecure() {
    return new SecureRandom();
}
````
This example method redirects the ``new Random()`` call to the ``makeSecure`` method.  
This replaces the original ``Random`` instance with a ``Secure Random``.

#### CWrapCatch
With the ``CWrapCatch`` annotation you can wrap a try-catch block around the entire method or an invoke instruction and handle the exception.
````java
@CWrapCatch("getResponseCode")
public int dontThrow(IOException e) {
    return 404;
}
````
This example method wraps the ``getResponseCode`` method in a try-catch block and handles the exception.  
The exception in the parameter is the exception catched by the try-catch block.  
The return type must be the one of the original method.  
You can even re-throw the exception or throw a new one.
````java
@CWrapCatch(value = "closeConnection", target = "Ljava/io/OutputStream;close()V")
public void dontThrow(IOException e) {
}
````
This example method wraps the ``OutputStream#close`` method call in a try-catch block and handles the exception.  
The exception in the parameter is the exception catched by the try-catch block.  
The return type must be the one of the original method call.  
You can even re-throw the exception or throw a new one.
