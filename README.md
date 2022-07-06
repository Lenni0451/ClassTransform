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

| Annotation Type | Description                                                                          |
|-----------------|--------------------------------------------------------------------------------------|
| CASM            | The access to the ClassNode of the entire class or a MethodNode of the wanted method |
| CInject         | Inject into any method at the given targets                                          |
| CModifyConstant | Modify a constant value in a method (null/int/long/float/double/string)              |
| COverride       | Override any method in the target class                                              |
| CRedirect       | Redirect a method call or a field get/put to your injection method                   |
| CWrapCatch      | Wrap a try-catch block around the entire method and handle the exception             |

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
<details>
    <summary>CTransformer</summary>

    TODO: example here
</details>
<details>
    <summary>CASM</summary>

    TODO: example here
</details>
<details>
    <summary>CInject</summary>

    TODO: example here
</details>
<details>
    <summary>CModifyConstant</summary>

    TODO: example here
</details>
<details>
    <summary>COverride</summary>

    TODO: example here
</details>
<details>
    <summary>CRedirect</summary>

    TODO: example here
</details>
<details>
    <summary>CWrapCatch</summary>

    TODO: example here
</details>
