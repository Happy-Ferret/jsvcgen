/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
**/
package com.gockelhut.jsvcgen.codegen

import java.io.File
import scala.util.{Failure, Success, Try}
import com.gockelhut.jsvcgen.loader.JsvcgenDescription
import scala.io.Source
import com.gockelhut.jsvcgen.model.ValidationException

case class CliConfig(description: File           = new File("."),
                     output:      File           = new File("-"),
                     generator:   String         = "java",
                     namespace:   String         = "com.example",
                     serviceBase: Option[String] = None
                    )

object Cli {
  def createGenerator(config: CliConfig) = config.generator match {
    case "java"     => new JavaCodeGenerator(config)
    case "validate" => new Validator(config)
  }
  
  def validateWith[T](r: => T): Either[String, T] = {
    Try(r) match {
      case Success(x) => Right(x)
      case Failure(x) => Left(x.getMessage())
    }
  }
  
  def getParser() = {
    val ModelUtil = com.gockelhut.jsvcgen.model.Util
    new scopt.OptionParser[CliConfig]("jsvcgen-generate") {
      head("jsvcgen")
      arg[File]("description")
        .text("JSON service description input file.")
        .required()
        .action { (x, c) => c.copy(description = x) }
      opt[File]('o', "output")
        .text("Output destination -- where to put the generated code.")
        .optional()
        .action { (x, c) => c.copy(output = x) }
      opt[String]('g', "generator")
        .text("Code generator to use.")
        .optional()
        .action { (x, c) => c.copy(generator = x) }
      opt[String]("namespace")
        .text("For namespace-based languages (such as Java), what namespace should the generated code be put in?")
        .optional()
        .action { (x, c) => c.copy(namespace = x) }
        .validate { x => validateWith(ModelUtil.validateNamespace(x)) }
      opt[String]("service-base")
        .text("When generating the output of a ServiceDefinition, the base class to use. " + 
              "The value \"default\" means use the generator's default")
        .optional()
        .action { (x, c) => c.copy(serviceBase = if (x.equals("default")) None else Some(x)) }
    }
  }
  
  def main(args: Array[String]): Unit = {
    getParser().parse(args, CliConfig()) map { config =>
      import org.json4s.jackson.JsonMethods
      
      // arguments are valid
      val generator = createGenerator(config)
      val service = JsvcgenDescription.load(JsonMethods.parse(Source.fromFile(config.description).mkString))
      try {
        generator.generate(service)
      } catch {
        case err:
          ValidationException => println(err.getMessage)
          System.exit(1)
      }
    } getOrElse {
      System.exit(1)
    }
  }
}
