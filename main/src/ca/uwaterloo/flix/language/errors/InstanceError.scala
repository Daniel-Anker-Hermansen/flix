/*
 * Copyright 2020 Matthew Lutze
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

package ca.uwaterloo.flix.language.errors

import ca.uwaterloo.flix.api.Flix
import ca.uwaterloo.flix.language.CompilationMessage
import ca.uwaterloo.flix.language.ast.{Ast, Scheme, SourceLocation, Symbol, Type}
import ca.uwaterloo.flix.language.fmt.{Audience, FormatScheme, FormatType, FormatTypeConstraint}
import ca.uwaterloo.flix.util.Formatter

/**
  * A common super-type for instance errors.
  */
sealed trait InstanceError extends CompilationMessage {
  val kind: String = "Instance Error"
}

object InstanceError {
  private implicit val audience: Audience = Audience.External

  /**
    * Error indicating that the types of two instances overlap.
    *
    * @param loc1 the location of the first instance.
    * @param loc2 the location of the second instance.
    */
  case class OverlappingInstances(loc1: SourceLocation, loc2: SourceLocation) extends InstanceError {
    def summary: String = "Overlapping instances."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |
         |${code(loc1, "the first instance was declared here.")}
         |
         |${code(loc2, "the second instance was declared here.")}
         |""".stripMargin
    }

    def loc: SourceLocation = loc1

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip: ")} Remove or change the type of one of the instances."
    })
  }

  /**
    * Error indicating that the type scheme of a definition does not match the type scheme of the signature it implements.
    *
    * @param sigSym   the mismatched signature
    * @param loc      the location of the definition
    * @param expected the scheme of the signature
    * @param actual   the scheme of the definition
    */
  case class MismatchedSignatures(sigSym: Symbol.SigSym, loc: SourceLocation, expected: Scheme, actual: Scheme)(implicit flix: Flix) extends InstanceError {
    def summary: String = "Mismatched signature."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |
         |Mismatched signature '${red(sigSym.name)}' required by class '${red(sigSym.clazz.name)}'.
         |
         |${code(loc, "mismatched signature.")}
         |
         |Expected scheme: ${FormatScheme.formatScheme(expected)}
         |Actual scheme:   ${FormatScheme.formatScheme(actual)}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Modify the definition to match the signature."
    })
  }

  /**
    * Error indicating the instance is missing a signature implementation.
    *
    * @param sig the missing signature.
    * @param loc the location of the instance.
    */
  case class MissingImplementation(sig: Symbol.SigSym, loc: SourceLocation) extends InstanceError {
    def summary: String = "Missing implementation."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |
         |Missing implementation of '${red(sig.name)}' required by class '${red(sig.clazz.name)}'.
         |
         |${code(loc, s"missing implementation")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Add an implementation of the signature to the instance."
    })
  }

  /**
    * Error indicating the instance has a definition not present in the implemented class.
    *
    * @param defn the extraneous definition.
    * @param loc  the location of the definition.
    */
  case class ExtraneousDefinition(defn: Symbol.DefnSym, loc: SourceLocation) extends InstanceError {
    def summary: String = "Extraneous implementation."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |
         |${code(loc, s"The signature ${defn.name} is not present in the class.")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Remove this definition from the instance."
    })
  }

  /**
    * Error indicating the duplicate use of a type variable in an instance type.
    *
    * @param tvar the duplicated type variable.
    * @param sym  the class symbol.
    * @param loc  the location where the error occurred.
    */
  case class DuplicateTypeVariableOccurrence(tvar: Type.Var, sym: Symbol.InstanceSym, loc: SourceLocation)(implicit flix: Flix) extends InstanceError {
    override def summary: String = "Duplicate type variable."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Duplicate type variable '${red(FormatType.formatType(tvar))}' in '${red(sym.name)}'.
         |
         |${code(loc, s"The type variable '${FormatType.formatType(tvar)}' occurs more than once.")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Rename one of the instances of the type variable."
    })

  }

  /**
    * Error indicating a complex instance type.
    *
    * @param tpe the complex type.
    * @param sym the class symbol.
    * @param loc the location where the error occurred.
    */
  case class ComplexInstanceType(tpe: Type, sym: Symbol.InstanceSym, loc: SourceLocation)(implicit flix: Flix) extends InstanceError {
    override def summary: String = "Complex instance type."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Complex instance type '${red(FormatType.formatType(tpe))}' in '${red(sym.name)}'.
         |${code(loc, s"complex instance type")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} An instance type must be a type constructor applied to zero or more distinct type variables or Boolean types."
    })
  }

  /**
    * Error indicating a type alias in an instance type.
    *
    * @param alias the type alias.
    * @param clazz the class symbol.
    * @param loc   the location where the error occurred.
    */
  case class IllegalTypeAliasInstance(alias: Symbol.TypeAliasSym, clazz: Symbol.InstanceSym, loc: SourceLocation) extends InstanceError {
    override def summary: String = "Type alias in instance type."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Illegal use of type alias '${red(alias.name)}' in instance declaration for '${red(clazz.name)}'.
         |${code(loc, s"illegal use of type alias")}
         |""".stripMargin

    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} A type class instance cannot use a type alias. Use the full type."
    })

  }

  /**
    * Error indicating an orphan instance.
    *
    * @param tpe the instance type.
    * @param sym the instance symbol.
    * @param loc the location where the error occurred.
    */
  case class OrphanInstance(tpe: Type, sym: Symbol.InstanceSym, loc: SourceLocation)(implicit flix: Flix) extends InstanceError {
    override def summary: String = "Orphan instance."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Orphan instance for type '${red(FormatType.formatType(tpe))}' in '${red(sym.name)}'.
         |${code(loc, s"orphan instance")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} An instance must be declared in the class's namespace or in the type's namespace."
    })
  }

  /**
    * Error indicating a missing super class instance.
    *
    * @param tpe        the type for which the super class instance is missing.
    * @param subClass   the symbol of the sub class.
    * @param superClass the symbol of the super class.
    * @param loc        the location where the error occurred.
    */
  case class MissingSuperClassInstance(tpe: Type, subClass: Symbol.InstanceSym, superClass: Symbol.ClassSym, loc: SourceLocation)(implicit flix: Flix) extends InstanceError {
    override def summary: String = s"Missing super class instance '$superClass'."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Missing super class instance '${red(superClass.name)}' for type '${red(FormatType.formatType(tpe))}'.
         |
         |>> The class '${red(subClass.name)}' extends the class '${red(superClass.name)}'.
         |>> If you provide an instance for '${red(subClass.name)}' you must also provide an instance for '${red(superClass.name)}'.
         |
         |${code(loc, s"missing super class instance")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Add an instance of '${superClass.name}' for '${FormatType.formatType(tpe)}'."
    })
  }

  /**
    * Error indicating an unlawful signature in a lawful class.
    *
    * @param sym the symbol of the unlawful signature.
    * @param loc the location where the error occurred.
    */
  case class UnlawfulSignature(sym: Symbol.SigSym, loc: SourceLocation) extends InstanceError {
    override def summary: String = s"Unlawful signature '$sym'."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Unlawful signature '${red(sym.name)}'.
         |
         |>> Each signature of a lawful class must appear in at least one law.
         |${code(loc, s"unlawful signature")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Create a law for '$sym' or mark the class as unlawful."
    })
  }

  /**
    * Error indicating the illegal placement of an override modifier.
    *
    * @param sym the def that the modifier was applied to.
    * @param loc the location where the error occurred.
    */
  case class IllegalOverride(sym: Symbol.DefnSym, loc: SourceLocation) extends InstanceError {
    override def summary: String = s"Illegal override of '$sym'."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Illegal override of '${red(sym.name)}'.
         |
         |>> Only signatures with default implementations can be overridden.
         |
         |${code(loc, s"illegal override")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Remove the modifier."
    })
  }

  /**
    * Error indicating a missing override modifier.
    *
    * @param sym the def that is missing the modifier.
    * @param loc the location where the error occurred.
    */
  case class UnmarkedOverride(sym: Symbol.DefnSym, loc: SourceLocation) extends InstanceError {
    override def summary: String = s"Unmarked override '$sym'."

    def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Unmarked override of '${red(sym.name)}'. This definition overrides a default implementation.
         |
         |${code(loc, s"unmarked override")}
         |""".stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Either add the `override` modifier or remove the definition."
    })
  }

  /**
    * An error indicating that a required constraint is missing from an instance declaration.
    *
    * @param tconstr    the missing constraint.
    * @param superClass the superclass that is the source of the constraint.
    * @param loc        the location where the error occurred.
    */
  case class MissingConstraint(tconstr: Ast.TypeConstraint, superClass: Symbol.ClassSym, loc: SourceLocation)(implicit flix: Flix) extends InstanceError {
    override def summary: String = s"Missing type constraint: ${FormatTypeConstraint.formatTypeConstraint(tconstr)}"

    override def message(formatter: Formatter): String = {
      import formatter._
      s"""${line(kind, source.name)}
         |>> Missing type constraint: ${FormatTypeConstraint.formatTypeConstraint(tconstr)}
         |
         |The constraint ${FormatTypeConstraint.formatTypeConstraint(tconstr)} is required because it is a constraint on super class ${superClass.name}.
         |
          |${code(loc, s"missing type constraint")}
      """.stripMargin
    }

    def explain(formatter: Formatter): Option[String] = Some({
      import formatter._
      s"${underline("Tip:")} Add the missing type constraint."
    })
  }

}
