package com.ergonlabs.ScalaForAndroid

import _root_.java.io.File
import _root_.org.apache.tools.ant._
import _root_.org.apache.tools.ant.types._
import _root_.org.apache.tools.ant.taskdefs._
import _root_.scala.tools.ant.Scalac
import _root_.proguard.ant._

class SetupScalaTask extends Task {
    var sfaPath = "${sdk.dir}/sfa"

    def setSfapath(value:String) {
        sfaPath = value
    }

    override def execute() {
        addProperties

        addCompileScalaTarget

        addTreeshakeTarget

        addTreeshakeDependencyToDex
    }

    private def addTreeshakeDependencyToDex {
        val targets = getProject.getTargets
        var d = targets.get("dex")
        if (d == null)
            d = targets.get("-dex")

        val dex = d.asInstanceOf[Target]
        dex addDependency "treeshake"
    }

    private def getPath(s:String) : String = getProject.replaceProperties(s)
    private def getFile(s:String) : File = getProject.resolveFile(getPath(s))

    private def addCompileScalaTarget {
        getProject.addTaskDefinition("Scalac", classOf[Scalac])
        val scala = getProject.createTask("Scalac").asInstanceOf[Scalac]
        scala setForce true
        scala setDeprecation "on"
        scala setSrcdir new Path(getProject, getPath("${sfa.src.dir}"))
        scala setIncludes "**/*.scala"
        scala setDestdir getFile("${sfa.bin.classes.dir}")
        
        val path = scala.createClasspath
        path.createPathElement setLocation getFile("${sfa.android.jar}")
        path addFileset newFileSet("${sfa.bin.classes.dir}", null)
        path addFileset newFileSet("${sfa.libs.dir}", "*.jar")
        path addFileset newFileSet("${sfa.dir}/libs", "scala-android.jar")

        val target = new Target
        target setName "compile-scala"
        target addDependency "compile"
        target setLocation new Location("ScalaForAndroid.Compile")
        target addTask scala

        getProject addTarget target
    }

    private def newFileSet(dir:String, includes:String) : FileSet = {
        val fs = new FileSet
        fs setDir getFile(dir)
        if (includes != null) fs.setIncludes(includes)
        return fs
    }

    private def addTreeshakeTarget {
        getProject.addTaskDefinition("ProGuardTask", classOf[ProGuardTask])
        val treeshake = getProject.createTask("ProGuardTask").asInstanceOf[ProGuardTask]
        treeshake setOptimize false
        treeshake setObfuscate false
        treeshake setWarn false
        treeshake setNote false

        treeshake addConfiguredInjar path("${sfa.bin.classes.dir}", null)
        treeshake addConfiguredInjar path("${sfa.dir}/libs/scala-android.jar", "!META-INF/MANIFEST.MF,!library.properties")

        treeshake addConfiguredOutjar path("${sfa.bin.classes.dir}.min", null)

        treeshake addConfiguredLibraryjar path("${sfa.android.jar}", null)
        treeshake addConfiguredLibraryjar path("${sfa.bin.classes.dir}", null)

        treeshake addConfiguredKeepclassmembers keep("${sfa.package}.**", "class", null)
        treeshake addConfiguredKeep keep("${sfa.package}.**", null, "public")

        val target = new Target
        target setName "treeshake"
        target addDependency "compile"
        target addDependency "compile-scala"
        target setLocation new Location("ScalaForAndroid.TreeShake")

        val mkdir = getProject.createTask("mkdir").asInstanceOf[Mkdir]
        mkdir setDir getFile("${sfa.bin.classes.dir}.min")
        target addTask mkdir

        if (getFile("${sfa.bin.classes.dir}/scala-android.jar").isFile()) {
          val rm1 = getProject.createTask("delete").asInstanceOf[Delete]
          rm1 setFile getFile("${sfa.bin.classes.dir}/scala-android.jar")
          target addTask rm1
        }

        if (getFile("${sfa.bin.classes.dir}.min/scala-android.jar").isFile()) {
          val rm2 = getProject.createTask("delete").asInstanceOf[Delete]
          rm2 setFile getFile("${sfa.bin.classes.dir}.min/scala-android.jar")
          target addTask rm2
        }

        val cp = getProject.createTask("copy").asInstanceOf[Copy]
        cp setFile  getFile("${sfa.bin.classes.dir}.min/scala-android.jar")
        cp setTofile getFile("${sfa.bin.classes.dir}/scala-android.jar")
        cp setFailOnError false

        target addTask treeshake
        target addTask cp
        
        getProject addTarget target
    }

    private def path(file:String, filter:String) : ClassPathElement = {
        val p = new ClassPathElement(getProject)
        p setFile getFile(file)
        if (filter != null)
            p setFilter filter
        return p
    }

    private def filter(name:String) : FilterElement = {
        val f = new FilterElement
        f setName name
        return f
    }

    private def keep(name:String, _type:String, access:String) : KeepSpecificationElement = {
        val k = new KeepSpecificationElement
        k setName getPath(name)
        if (_type != null) {
            k setType _type
            val m = new MemberSpecificationElement
            m setName "*"
            k addConfiguredMethod m
        }
        if (access != null)
            k setAccess access
        return k
    }

    private def addProperties {
        getProject getProperty "target" match {
            case "android-3" => addAndroid15properties // v1.5
            case "android-4" => addAndroid21properties // v1.6
            case "android-6" => addAndroid21properties // v2.0.1
            case "android-7" => addAndroid21properties // v2.1
            case _ => throw new BuildException("Unsupported android build target. Only 1.5, 1.6, 2.0.1, and 2.1 are supported.")
        }
    }

    private def addAndroid15properties {
        addAndroidProperties("source-location", "out-classes-location", "android-jar", "out-folder", "application-package", "external-libs-folder")
    }

    private def addAndroid21properties {
        addAndroidProperties("source.absolute.dir", "out.classes.absolute.dir", "android.jar", "out.absolute.dir", "manifest.package", "external.libs.absolute.dir")
    }

    private def addAndroidProperties(srcDirName:String, outClassesDirName:String, androidJarName:String, outDirName:String, outPackage:String, externalLibDirName:String) {
        val proj = getProject

        proj.setProperty("sfa.dir", getPath(sfaPath))
        proj.setProperty("sfa.src.dir", getPath(proj getProperty srcDirName))
        proj.setProperty("sfa.bin.classes.dir", getPath(proj getProperty outClassesDirName))
        proj.setProperty("sfa.android.jar", getPath(proj getProperty androidJarName))
        proj.setProperty("sfa.bin.dir", getPath(proj getProperty outDirName))
        proj.setProperty("sfa.package", getPath(proj getProperty outPackage))
        proj.setProperty("sfa.libs.dir", getPath(proj getProperty externalLibDirName))
    }
}
