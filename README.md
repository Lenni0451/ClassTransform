# ClassTransform
A lightweight, mixin like injection lib using ASM.\
The usage is like Mixins. You can almost copy-paste mixins code, and it works.\
Or you can use [MixinsTranslator](#copy-pasting-mixins) and copy-paste them almost 1:1.

## Usage
Check out the [wiki](https://github.com/Lenni0451/ClassTransform/wiki) for more information.

### Gradle/Maven
To use ClassTransform with Gradle/Maven you can get it from [maven central](https://mvnrepository.com/artifact/net.lenni0451.classtransform).\
You can also find instructions how to implement it into your build script there.

### Jar File
If you just want the latest jar file you can download it from my [Jenkins](https://build.lenni0451.net/job/ClassTransform/).

## Why?
I wanted a lightweight version of mixins which I can easily add into any program.\
It even contains a custom ClassLoader to inject into classes before loading them if you can't resort to agents.
