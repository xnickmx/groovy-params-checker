groovy-params-checker
==============

Automatic Groovy parameter checker
--------------

What is this?
--------------
A tool to automatically check Groovy class method parameters to make sure they aren't null or empty. 

Why would you use it?
--------------
You would use this if you:

* Like your code to [fail fast](http://en.wikipedia.org/wiki/Fail-fast)
* Don't like writing boilerplate code
* Don't like writing extra unit tests
* Need a solution that supports Groovy
* Prefer as solution that requires zero configuration to use

How to Use
--------------

1. Include this project in your code via sources, Gradle, Maven, etc.
2. Annotate your Groovy classes with @ParamsNotNullNotEmpty. 
3. At compile time, code will be added into each of your methods to check each parameter to make sure it is not null 
and in the case of Collections, Maps and Strings, it is not empty.
4. At run time, if a parameter is null or empty, it will throw an IllegalArgumentException with details about the
null or empty parameter. 
5. To disable the annotation at compile time, set the ParamsNotNullNotEmpty.NoOpMode property to true.

Example Usage
--------------
See the [integration tests](https://github.com/xnickmx/groovy-params-checker/blob/master/src/it/groovy/com/faceture/gparamschecker/ParamsNotNullNotEmptyIT.groovy)

Maven Central
--------------

Include in your Ant/Gradle/Grails/Maven project using binaries from [Maven central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22groovy-params-checker%22)

<dependency>
    <groupId>com.faceture</groupId>
    <artifactId>groovy-params-checker</artifactId>
    <!-- be sure to check Maven central for latest version -->
    <version>1.0.27</version>
</dependency>


Informed and Inspired by
--------------
This project was informed and inspired by other great work:

1. Groovy 1.6 AST Transformation Example [blog](http://blog.kartikshah.com/2009/03/groovy-16-ast-transformation-example_5323.html) 
and [code](https://github.com/kartikshah/ast-assert-not-null) by Kartik Shah.

2. "AST Transformations: Prerequisites and Annotations" [blog](http://joesgroovyblog.blogspot.com/2011/09/ast-transformations-prerequisites-and.html)
and [code](https://github.com/jbaumann/ASTTransformation-Requires) by Joachim Baumann.

3. GContracts [code](https://github.com/andresteingress/gcontracts) by Andre Steingress.
