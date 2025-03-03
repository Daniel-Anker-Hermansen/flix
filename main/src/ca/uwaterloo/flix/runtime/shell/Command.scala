/*
 * Copyright 2017 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.uwaterloo.flix.runtime.shell

import org.jline.terminal.Terminal

/**
  * A common super-type for commands.
  */
sealed trait Command

object Command {

  /**
    * Does literally nothing.
    */
  case object Nop extends Command

  /**
    * Reloads all source paths.
    */
  case object Reload extends Command

  /**
    * Displays documentation about the fqn s
    */
  case class Doc(s: String) extends Command

  /**
    * Creates a new project in the current directory
    */
  case object Init extends Command

  /**
    * Checks the current project for errors.
    */
  case object Check extends Command

  /**
    * Builds the current project.
    */
  case object Build extends Command

  /**
    * Builds a jar file from the current project.
    */
  case object Jar extends Command

  /**
    * Builds an fpkg file from the current project.
    */
  case object Fpkg extends Command

  /**
    * Runs the benchmarks for the current project.
    */
  case object Bench extends Command

  /**
    * Runs the tests for the current project.
    */
  case object Test extends Command

  /**
    * Installs the Flix package from the given GitHub owner/repo
    */
  case class Install(project: String) extends Command

  /**
    * Terminates the shell.
    */
  case object Quit extends Command

  /**
    * Prints helpful information about the available commands.
    */
  case object Help extends Command

  /**
    * Praise Le Toucan.
    */
  case object Praise extends Command

  /**
    * Eval source code.
    */
  case class Eval(s: String) extends Command

  /**
    * Reload and eval source code.
    */
  case class ReloadAndEval(s: String) extends Command

  /**
    * Unknown command.
    */
  case class Unknown(s: String) extends Command

  /**
    * Parses the given `input` into a command.
    */
  def parse(input: String)(implicit terminal: Terminal): Command = {
    //
    // Eof
    //
    if (input == null)
      return Command.Quit

    //
    // Nop
    //
    if (input.trim == "")
      return Command.Nop

    //
    // Reload
    //
    if (input == ":r" || input == ":reload")
      return Command.Reload

    //
    // Doc
    //
    val docPattern = raw":d(oc)?\s+(\S+)\s*".r
    input match {
      case docPattern(_, s) => return Command.Doc(s)
      case _ => // no-op
    }

    //
    // Init
    //
    if (input == ":init")
      return Command.Init

    //
    // Check
    //
    if (input == ":check" || input == ":c")
      return Command.Check

    //
    // Build
    //
    if (input == ":build" || input == ":b")
      return Command.Build

    //
    // Jar
    //
    if (input == ":build-jar" || input == ":jar")
      return Command.Jar

    //
    // Fpkg
    //
    if (input == ":build-pkg" || input == ":pkg")
      return Command.Fpkg

    //
    // Bench
    //
    if (input == ":benchmark" || input == ":bench")
      return Command.Bench

    //
    // Eval
    //
    if (input.startsWith(":eval"))
      return Command.ReloadAndEval(input.drop(":eval".length + 1))

    //
    // Test
    //
    if (input == ":test" || input == ":t")
      return Command.Test

    //
    // Install
    //
    val installPattern = raw":install\s+(\S+)\s*".r
    input match {
      case installPattern(s) => return Command.Install(s)
      case _ => // no-op
    }

    //
    // Quit
    //
    if (input == ":quit" || input == ":q")
      return Command.Quit

    //
    // Help
    //
    if (input == ":help" || input == ":h" || input == ":?")
      return Command.Help

    //
    // Praise
    //
    if (input == ":praise")
      return Command.Praise

    //
    // Eval or Unknown?
    //
    if (input.startsWith(":"))
      Command.Unknown(input)
    else
      Command.Eval(input)
  }

}
