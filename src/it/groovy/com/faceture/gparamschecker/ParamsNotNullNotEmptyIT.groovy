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

package com.faceture.gparamschecker;

/**
 * Integration test of the @ParamsNotNullNotEmpty annotation. Examples of the types of exceptions that will be thrown
 * in different cases.
 */
class ParamsNotNullNotEmptyIT extends GroovyTestCase {

    ParamsNotNullNotEmptyExample paramsNotNullNotEmptyExample

    // params
    final String aStr = "my string"
    final Float aFloat = 1.23f
    final List aList = ["item1", "item2"]
    final Map aMap = [foo: "bar"]
    final Set aSet = [1, 2, 3] as Set

    void setUp() {
        super.setUp()

        // create test class
        paramsNotNullNotEmptyExample = new ParamsNotNullNotEmptyExample(aStr)
    }

    /////////////////////////////////////////////////////////////
    // Make sure ParamsNotNullNotEmpty works on constructors
    /////////////////////////////////////////////////////////////

    void testConstructorFailsDueToNullStr() {
        shouldFail(IllegalArgumentException, {new ParamsNotNullNotEmptyExample(null)})
    }

    void testConstructorFailsDueToEmptyStr() {
        shouldFail(IllegalArgumentException, {new ParamsNotNullNotEmptyExample("")})
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Make sure ParamsNotNullNotEmpty works on statically typed method with params that can and can't be empty
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void testFailsDueToNullString() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(null, aFloat,
                aList, aMap, aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aStr' is null./
    }

    void testFailsDueToEmptyString() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example("", aFloat, aList,
                aMap, aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aStr' is empty./
    }

    void testFailsDueToNullFloat() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, null, aList,
                aMap, aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aFloat' is null./
    }

    void testFailsDueToNullList() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, aFloat, null,
                aMap, aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aList' is null./
    }

    void testFailsDueToEmptyList() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, aFloat, [],
                aMap, aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aList' is empty./
    }

    void testFailsDueToNullMap() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, aFloat,
                aList, null, aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aMap' is null./
    }

    void testFailsDueToEmptyMap() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, aFloat,
                aList, [:], aSet)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aMap' is empty./
    }

    void testFailsDueToNullSet() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, aFloat,
                aList, aMap, null)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aSet' is null./
    }

    void testFailsDueToEmptySet() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.example(aStr, aFloat,
                aList, aMap, [] as Set)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.example call failed because parameter 'aSet' is empty./
    }

    /////////////////////////////////////////////////////////////
    // Make sure ParamsNotNullNotEmpty works on static methods
    /////////////////////////////////////////////////////////////

    void testStaticMethodFailsDueToNullStr() {
        String errorMessage = shouldFail(IllegalArgumentException, {ParamsNotNullNotEmptyExample.exampleStatic(null)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.exampleStatic call failed because parameter 'aStr' is null./
    }

    void testStaticMethodFailsDueToEmptyStr() {
        String errorMessage = shouldFail(IllegalArgumentException, {ParamsNotNullNotEmptyExample.exampleStatic("")})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.exampleStatic call failed because parameter 'aStr' is empty./
    }

    ///////////////////////////////////////////////////////////////////////////
    // Make sure ParamsNotNullNotEmpty works on dynamically-typed methods
    ///////////////////////////////////////////////////////////////////////////

    void testExampleDynamicTypeParamNull() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.exampleDynamicParamType(null)})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.exampleDynamicParamType call failed because parameter 'myVar' is null./
    }

    void testExampleDynamicTypeParamEmpty() {
        String errorMessage = shouldFail(IllegalArgumentException, {paramsNotNullNotEmptyExample.exampleDynamicParamType("")})

        assert errorMessage =~ /ParamsNotNullNotEmptyExample.exampleDynamicParamType call failed because parameter 'myVar' is empty./
    }

    void testExampleDynamicTypeParamHappyPath() {
        def myVar = 1.23
        assertEquals myVar.toString(), paramsNotNullNotEmptyExample.exampleDynamicParamType(myVar)
    }

    void testHappyPath() {
        assertEquals "aStr: $aStr, aFloat: $aFloat, aList: $aList, aMap: $aMap, aSet: $aSet",
                paramsNotNullNotEmptyExample.example(aStr, aFloat, aList, aMap, aSet)
    }
}
