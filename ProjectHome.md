# Scala for Android #

Scala for Android is meant to make it super easy to write android apps in scala. You do the installation steps once, and then add a few lines to your projects' build.xml files, and you are ready to go!


---


## Installation: ##

Note: if you clone the source with hg, in many cases you can just type "ant install" (though you may need to modify the "sdk.dir" property in build.xml to point at your android sdk directory).

  1. In your android sdk directory, create directory named sfa, and in that directory create another directory named libs. So if your android sdk directory were c:\android-sdk, then you would have c:\android-sdk\sfa\libs.
  1. Put all the .jar files in the ScalaForAndroid.zip file into the sfa/libs
directory.

## How to Use: ##

  1. Use the android sdk to create a project
  1. In your build.xml file:
    * Add the following inside the `<path id="android.antlibs">`
```
        <pathelement path="${sdk.dir}/sfa/libs/ScalaForAndroid.jar" />
        <pathelement path="${sdk.dir}/sfa/libs/scala-library.jar" />
        <pathelement path="${sdk.dir}/sfa/libs/scala-compiler.jar" />
        <pathelement path="${sdk.dir}/sfa/libs/proguard.jar" />
```
    * Add the following after `<setup />`
```
        <taskdef name="setupscala"
           classname="com.ergonlabs.ScalaForAndroid.SetupScalaTask"
            classpathref="android.antlibs"
        />
        <setupscala sfapath="..\ScalaForAndroid" />
```
  1. Put some .scala files in your src directory and build using ant.


---


## File Size ##

A common question is about the size of the resulting .apk file. The android-scala.jar file is pretty big so this is an important consideration. AndroidForScala includes a treeshaker (proguard) that removes all the parts of the android-scala library that you don't use. For a simple "hello world" the scala library takes up about 10kb, and for a larger real world app (500kb), the library takes up more like 25kb.