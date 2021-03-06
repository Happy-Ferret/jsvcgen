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

import org.json4s.JsonAST.JValue

object Util {

  import java.io.FileInputStream

  import org.fusesource.scalate.{ TemplateEngine, TemplateSource }
  import org.json4s.jackson.JsonMethods

  import scala.io.Source

  def camelCase( src: String, firstUpper: Boolean ): String = {
    val out = new StringBuilder( )
    var nextUpper = firstUpper
    var isFirst = true
    for (c <- src) {
      if (c == '_' || c == '-' || c == '#') {
        nextUpper = true
      } else if (nextUpper) {
        out.append( c.toUpper )
        nextUpper = false
        isFirst = false
      } else {
        out.append( if (isFirst) c.toLower else c )
        isFirst = false
      }
    }
    out.result( )
  }

  def underscores( src: String ): String = {
    val out = new StringBuilder( )
    var sawUpper = true
    for (c <- src) {
      if(c == '"' || c == '''){
        out.append(c)
      }
      else if (sawUpper) {
        if (c.isUpper) {
          out.append( c.toLower )
        } else if (c == '-' || c == '#'){
          out.append( '_' )
        } else {
          sawUpper = false
          out.append( c )
        }
      } else {
        if (c.isUpper) {
          sawUpper = true
          out.append( '_' )
          out.append( c.toLower )
        } else if (c == '-' || c == '#'){
          out.append( '_' )
        } else {
          out.append( c )
        }
      }
    }
    out.result( )
  }

  def whitespaceOffset(n:Int) = {
    " " * n
  }

  def loadJson( path: String ): JValue =
    JsonMethods.parse( Source.fromFile( path ).mkString )

  def loadJsonAs[T]( path: String )( implicit mf: Manifest[T] ) = {
    implicit val formats = org.json4s.DefaultFormats
    loadJson( path ).extract[T]
  }

  def loadResource( path: String ): String =
    Source
      .fromInputStream( Option( getClass.getResourceAsStream( path ) )
      .getOrElse( new FileInputStream( path ) ) )
      .mkString

  def loadTemplate( path: String ): TemplateSource = {
    TemplateSource.fromText( path, loadResource( path ) )
  }

  def layoutTemplate( path: String, attributes: Map[String, Any] ): String = {
    val templateEngine = new TemplateEngine {
      escapeMarkup = false
      allowCaching = true
      allowReload = false
    }

    templateEngine.layout( templateEngine.load(loadTemplate( path ) ).source, attributes )
  }

  def pathForNamespace( namespace: String ) = namespace.replaceAll( "\\.", "/" )

  def lastWhitespace(line: String, max: Int): Int =  {
    if(line.length >= max)
      line.substring(0, max+1).lastIndexOf(' ')
    else
      line.lastIndexOf(' ')

  }

  def trimTrailing(line: String): String =  line.replaceAll("""(?m)\s+$""", "")

  def stringJoin( input: List[String], sep: String ): String = input match {
    case Nil => ""
    case last :: Nil => last
    case s :: rest => s + sep + stringJoin( rest, sep )
  }

  def removeEscapeFlags(line: String): String = {
    line.replaceAll(">>>","".replaceAll("<<<",""))
  }
}
