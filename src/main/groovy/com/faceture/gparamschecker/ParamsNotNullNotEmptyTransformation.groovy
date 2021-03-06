/*
 * Copyright (c) 2014 Faceture Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.faceture.gparamschecker

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.Variable
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * The AST transformation that modifies code that is annotated with @ParamsNotNullNotEmpty.
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class ParamsNotNullNotEmptyTransformation implements ASTTransformation {

    private final AstBuilder astBuilder = new AstBuilder()

    private static final int IS_EMPTY_CHECK_DYNAMIC = 1
    private static final int IS_EMPTY_CHECK_STATIC = 2
    private static final int IS_EMPTY_CHECK_NONE = 3

    private static final String AUTO_GEN_MSG = "Auto-generated by @ParamsNotNullNotEmpty:"

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

        if (System.getProperty("ParamsNotNullNotEmpty.NoOpMode") == "true") {
            // Running in no-op mode -- do nothing
            // This is useful when running code coverage tests -- this means the null and empty checks will not be
            // added.
            return
        }

        // per the documentation: http://groovy.codehaus.org/gapi/org/codehaus/groovy/transform/ASTTransformation.html
        // index 0 is the annotation node
        // index 1 is the class/method/etc node
        AnnotationNode annotationNode = (AnnotationNode)astNodes[0]
        ClassNode classNode = (ClassNode)astNodes[1]
        String className = classNode.getName()

        // get the methods in this class
        List<MethodNode> methodNodes = classNode.getMethods()

        // get just the public methods
        Collection<MethodNode> publicNonAbstractMethods = methodNodes.findAll{ MethodNode methodNode ->
            methodNode.isPublic() && !methodNode.isAbstract()
        }

        // get the public, non-abstract constructors
        List<ConstructorNode> constructors = classNode.getDeclaredConstructors()
        Collection<ConstructorNode> publicNonAbstractConstructors = constructors.findAll { ConstructorNode constructorNode ->
            constructorNode.isPublic() && !constructorNode.isAbstract()
        }

        // all of the methods to check
        Collection<MethodNode> methodsToCheck = []
        methodsToCheck.addAll(publicNonAbstractMethods)
        methodsToCheck.addAll(publicNonAbstractConstructors)

        // add an error check for each param on each method
        methodsToCheck.each { MethodNode methodNode ->

            // get the parameters to this method
            Parameter[] parameters = methodNode.getParameters()
            String methodName = methodNode.getName()

            // get the existing code from the method
            BlockStatement methodCode = (BlockStatement)methodNode.getCode()
            List<Statement> methodStatements = methodCode.getStatements()

            // for each parameter...
            parameters.eachWithIndex { Parameter parameter, int index ->

                // get the paramschecker code
                BlockStatement preconditionCheck = getPreconditionCheckStatement(className, methodName, parameter)

                // add the paramschecker code in order to the method
                methodStatements.add(index, preconditionCheck)
            }
        }
    }

    private BlockStatement getPreconditionCheckStatement(String className, String methodName, Parameter parameter) {
        String paramName = parameter.getName()


        String checkNullSource = """
            if (null == ${paramName}) {
                throw new IllegalArgumentException("$AUTO_GEN_MSG $className.$methodName call failed because parameter '$paramName' is null.")
            }
        """

        String isEmptySource
        int isEmptyCheckType
        if (parameter.isDynamicTyped()) {
            // dynamically typed param, so we need to check the type at run time
            isEmptySource =
                """else if ((${paramName} instanceof java.util.Collection || ${paramName} instanceof java.lang.String ||
                      ${paramName} instanceof java.util.Map) && ${paramName}.isEmpty()) {
                        throw new IllegalArgumentException("$AUTO_GEN_MSG $className.$methodName call failed because parameter '$paramName' is empty.")
                   }"""

            isEmptyCheckType = IS_EMPTY_CHECK_DYNAMIC
        }
        else {
            // statically typed

            Class parameterClass = parameter.getType().getTypeClass()

            if (Collection.isAssignableFrom(parameterClass) || String.isAssignableFrom(parameterClass) ||
                    Map.isAssignableFrom(parameterClass))
            {
                // the parameter's class is a Collection, String or Map, or a subclass of one of those classes
                // can just directly call the isEmpty method
                isEmptySource =
                    """else if (${paramName}.isEmpty()) {
                            throw new IllegalArgumentException("$AUTO_GEN_MSG $className.$methodName call failed because parameter '$paramName' is empty.")
                       }"""

                isEmptyCheckType = IS_EMPTY_CHECK_STATIC
            }
            else {
                // we don't want to do this isEmpty check on this param
                isEmptySource = "";

                isEmptyCheckType = IS_EMPTY_CHECK_NONE
            }
        }

        // put together our source code
        String source = checkNullSource + isEmptySource

		List<ASTNode> astNodes = astBuilder.buildFromString(CompilePhase.SEMANTIC_ANALYSIS, false, source)
		BlockStatement blockStatement = (BlockStatement)astNodes[0]

        // There is a problem with the AST returned by the builder -- it includes DynamicVariables. These variables
        // can't be compiled by the static compiler, which makes this code break if you try to use it with
        // @CompileStatic. The variables are Dynamic because they are not defined in the string source that is passed in.
        // They are not defined there because they are method parameters and thus defined in the method signature.
        // The work around is to modify the AST that was returned and replace the DynamicVariables with static Variables.

        // Our variable
        ClassNode paramType = parameter.getType()
        Variable variable = new VariableExpression(paramName, paramType)

        // this code: if (null == ${paramName})
        IfStatement ifStatement = (IfStatement)blockStatement.getStatements()[0]

        // this code: null == ${paramName}
        BinaryExpression nullEqualsParam = ((BinaryExpression)(ifStatement.booleanExpression.expression))

        // fix the null check variable
        nullEqualsParam.rightExpression = variable

        if (IS_EMPTY_CHECK_DYNAMIC == isEmptyCheckType) {
            // this is for dynamically typed params

            // this code: "else if ((${paramName} instanceof java.util.Collection || ${paramName} instanceof java.lang.String || ${paramName} instanceof java.util.Map) && ${paramName}.isEmpty())"
            IfStatement elseIfIsEmpty = (IfStatement)ifStatement.elseBlock

            // this code: ((${paramName} instanceof java.util.Collection || ${paramName} instanceof java.lang.String || ${paramName} instanceof java.util.Map) && ${paramName}.isEmpty())
            BinaryExpression isCollectionOrIsStringAndIsEmpty = (BinaryExpression)elseIfIsEmpty.booleanExpression.expression

            // this code: "(${paramName} instanceof java.util.Collection || ${paramName} instanceof java.lang.String || ${paramName} instanceof java.util.Map)"
            BinaryExpression allThreeInstanceOfExpressions = (BinaryExpression)isCollectionOrIsStringAndIsEmpty.leftExpression

            // this code: ${paramName} instanceof java.util.Collection || ${paramName} instanceof java.lang.String
            BinaryExpression instanceOfCollectionInstanceOfStringExpression = (BinaryExpression)allThreeInstanceOfExpressions.leftExpression

            // this code: ${paramName} instanceof java.util.Collection
            BinaryExpression instanceOfCollectionExpression = (BinaryExpression)instanceOfCollectionInstanceOfStringExpression.leftExpression

            // this code: ${paramName} instanceof java.lang.String
            BinaryExpression instanceOfStringExpression = (BinaryExpression)instanceOfCollectionInstanceOfStringExpression.rightExpression

            // this code: ${paramName} instanceof java.lang.String
            BinaryExpression instanceOfMapExpression = (BinaryExpression)allThreeInstanceOfExpressions.rightExpression

            // this code: ${paramName}.isEmpty()
            MethodCallExpression isEmptyCall = (MethodCallExpression)isCollectionOrIsStringAndIsEmpty.rightExpression

            // replace all of the DynamicVariables with our Variable
            instanceOfCollectionExpression.leftExpression = variable
            instanceOfStringExpression.leftExpression = variable
            instanceOfMapExpression.leftExpression = variable
            isEmptyCall.objectExpression = variable
        }
        else if (IS_EMPTY_CHECK_STATIC == isEmptyCheckType) {
            // this code:  else if (${paramName}.isEmpty())
            IfStatement elseIfIsEmpty = (IfStatement)ifStatement.elseBlock

            // this code ${paramName}.isEmpty()
            MethodCallExpression isEmptyCall = (MethodCallExpression)elseIfIsEmpty.booleanExpression.expression

            // replace the DynamicVariable with our Variable
            isEmptyCall.objectExpression = variable
        }

        return blockStatement
    }
}
