/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  **/
package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.codegen.TestHelper._
import com.solidfire.jsvcgen.loader.JsvcgenDescription.{DocumentationSerializer, MemberSerializer, ParameterSerializer, ReturnInfoSerializer, ServiceDefinitionSerializer, StabilityLevelSerializer, TypeUseSerializer}
import com.solidfire.jsvcgen.model.{ServiceDefinition, TypeDefinition}
import org.json4s.DefaultFormats
import org.scalatest.{Matchers, WordSpec}


class JavaCodeFormatterTests extends WordSpec with Matchers {

  implicit def formats = DefaultFormats +
    StabilityLevelSerializer +
    new DocumentationSerializer() +
    new MemberSerializer() +
    new ParameterSerializer() +
    new ReturnInfoSerializer() +
    new ServiceDefinitionSerializer() +
    new TypeUseSerializer()

  val formatter = new JavaCodeFormatter( buildOptions.copy( namespace = "testNameSpace" ), buildServiceDefinition )
  val simpleJson = Descriptions.getDescriptionJValue("simple.json")
  val simpleService = simpleJson.extract[ServiceDefinition]
  val javaFormatter = new JavaCodeFormatter( buildOptions.copy( namespace = "testNameSpace" ), simpleService )
  val csharpFormatter = new CSharpCodeFormatter( buildOptions.copy( namespace = "testNameSpace" ), simpleService )

  "buildExtends" should {
    "Generate types with no inheritance or interface" in {
      val typeDefinition = new TypeDefinition(name = "SubType")
      val classDefinition = formatter.buildExtends(typeDefinition, buildOptions)
      classDefinition should be("")
    }

    "Generate types with inheritance" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"))
      val classDefinition = formatter.buildExtends(typeDefinition, buildOptions)
      classDefinition should be("extends SuperType")
    }
  }

  "dictionary alias" should {
    "be right" in {
      val clusterHardwareInfoWithAlias =  simpleService.types.find(t => t.name == "ClusterHardwareInfoWithAlias").get
      var javaStrings: List[String] = for (member <- clusterHardwareInfoWithAlias.members) yield s"${member.name} private ${javaFormatter.getTypeName(member.typeUse)} ${javaFormatter.getFieldName(member)}"
      var csharpStrings: List[String] = for (member <- clusterHardwareInfoWithAlias.members) yield csharpFormatter.buildMember(member)

      javaStrings.contains("nodes private java.util.Map<String,Attributes> nodes") should be (true)
    }
  }

  "addImplements" should {
    "Generate types with one interface" in {
      val typeDefinition = new TypeDefinition(name = "SubType", implements = Some(List("IImplement")))
      val classDefinition = formatter.addImplements(typeDefinition)
      classDefinition should be (", IImplement")
    }

    "Generate types with inheritance and one interface" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"), implements = Some(List("IImplement")))
      val classDefinition = formatter.addImplements(typeDefinition)
      classDefinition should be (", IImplement")
    }

    "Generate types with inheritance and two interfaces" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"), implements = Some(List("IImplement", "IInterface")))
      val classDefinition = formatter.addImplements(typeDefinition)
      classDefinition should be (", IImplement, IInterface")
    }
  }

  "getTypeName(String)" should {
    "map wrapper types when primitives are not allowed" in {
      formatter.getTypeName( "boolean" ) should be( "Boolean" )
      formatter.getTypeName( "integer" ) should be( "Long" )
      formatter.getTypeName( "long" ) should be( "Long" )
      formatter.getTypeName( "number" ) should be( "Double" )
      formatter.getTypeName( "float" ) should be( "Double" )
    }

    "map string, regardless of case, to String" in {
      formatter.getTypeName( "string" ) should be( "String" )
      formatter.getTypeName( "String" ) should be( "String" )
    }

    "map hashtable, regardless of case, to java.util.Map<String, Object>" in {
      formatter.getTypeName( "hashtable" ) should be( "java.util.Map<String, Object>" )
    }

    "map object, regardless of case, to Object" in {
      formatter.getTypeName( "object" ) should be( "Object" )
    }

    "map types to base alias types" in {
      formatter.getTypeName( "yesOrNo" ) should be( "Boolean" )
      formatter.getTypeName( "uint64" ) should be( "Long" )
      formatter.getTypeName( "uint32" ) should be( "Long" )
      formatter.getTypeName( "size_t" ) should be( "Long" )
      formatter.getTypeName( "ID" ) should be( "Long" )
      formatter.getTypeName( "bigID" ) should be( "Long" )
      formatter.getTypeName( "smallID" ) should be( "Long" )
      formatter.getTypeName( "ratio" ) should be( "Double" )
      formatter.getTypeName( "precision" ) should be( "Double" )
      formatter.getTypeName( "name" ) should be( "String" )
      formatter.getTypeName( "UUID" ) should be( "java.util.UUID" )
    }

    "map optional types to alias types even if canBePrimitive" in {
      formatter.getTypeName( "maybeYesOrNo" ) should be( "Optional<Boolean>" )
      formatter.getTypeName( "someID" ) should be( "Optional<Long>" )
      formatter.getTypeName( "someBigID" ) should be( "Optional<Long>" )
      formatter.getTypeName( "someSmallID" ) should be( "Optional<Long>" )
      formatter.getTypeName( "someRatio" ) should be( "Optional<Double>" )
      formatter.getTypeName( "somePrecision" ) should be( "Optional<Double>" )
    }

    "map non-aliased, non-primitive types to capitalized case" in {
      formatter.getTypeName( "myType" ) should be( "MyType" )
    }
  }

  "getTypeName(TypeDefinition)" should {

    "map optional types to alias wrapper types" in {
      formatter.getTypeName( maybeYesOrNo ) should be( "Optional<Boolean>" )
      formatter.getTypeName( someID ) should be( "Optional<Long>" )
      formatter.getTypeName( someBigID ) should be( "Optional<Long>" )
    }
  }

  "deserialize TypeUse" should {
    "be an array when type is simple string" in {
      simpleService.types.find(_.name == "User").get.members.find(_.name == "username").get.typeUse.isArray should be (false)
    }
    "not be an array when type is object with only name field present" in {
      simpleService.types.find(_.name == "User").get.members.find(_.name == "notAnArray").get.typeUse.isArray should be (false)
     }
    "be an array when type is an array of string" in {
      simpleService.types.find(_.name == "User").get.members.find(_.name == "isAnArray").get.typeUse.isArray should be (true)
    }
    "not be an array when type is object with name and dictionaryType is present" in {
      simpleService.types.find(_.name == "ListFooPortInfoResult").get.members.find(_.name == "FooPortInfoResult").get.typeUse.isArray should be (false)
    }
  }

  "getTypeName(TypeUse)" should {
    "map array types to alias primitive array types" in {
      formatter.getTypeName( yesOrNo.alias.get.copy( isArray = true ) ) should be( "Boolean[]" )
      formatter.getTypeName( uint64.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( uint32.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( size_t.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( ID.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( bigID.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( smallID.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( ratio.alias.get.copy( isArray = true ) ) should be( "Double[]" )
      formatter.getTypeName( precision.alias.get.copy( isArray = true ) ) should be( "Double[]" )
    }

    "map optional types to alias wrapper types" in {
      formatter.getTypeName( maybeYesOrNo.alias.get ) should be( "Optional<Boolean>" )
      formatter.getTypeName( someID.alias.get ) should be( "Optional<Long>" )
      formatter.getTypeName( someBigID.alias.get ) should be( "Optional<Long>" )
      formatter.getTypeName( someSmallID.alias.get ) should be( "Optional<Long>" )
      formatter.getTypeName( someRatio.alias.get ) should be( "Optional<Double>" )
      formatter.getTypeName( somePrecision.alias.get ) should be( "Optional<Double>" )
    }

    "map optional array types to alias wrapper array types" in {
      formatter.getTypeName( maybeYesOrNo.alias.get.copy( isArray = true ) ) should be( "Optional<Boolean[]>" )
      formatter.getTypeName( someID.alias.get.copy( isArray = true ) ) should be( "Optional<Long[]>" )
      formatter.getTypeName( someBigID.alias.get.copy( isArray = true ) ) should be( "Optional<Long[]>" )
      formatter.getTypeName( someSmallID.alias.get.copy( isArray = true ) ) should be( "Optional<Long[]>" )
      formatter.getTypeName( someRatio.alias.get.copy( isArray = true ) ) should be( "Optional<Double[]>" )
      formatter.getTypeName( somePrecision.alias.get.copy( isArray = true ) ) should be( "Optional<Double[]>" )
    }
  }
}
