/*
 * Copyright 2017 Magnus Madsen
 * Copyright 2022 Jonathan Lindegaard Starup
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
namespace Console {

    ///
    /// Returns true if there is a `Console` object associated with the current JVM.
    ///
    pub def hasConsole(): Bool \ IO =
        import static java.lang.System.console(): ##java.io.Console \ IO;
        not Object.isNull(console())

    ///
    /// Reads a single line from the console.
    ///
    /// Returns `Err` if no console is present, there is an IO error, or EOF is found.
    ///
    /// See also `System/StdIn.readLines`.
    ///
    pub def readLine(): Result[String, String] \ IO = {
        import java.io.Console.readLine(): String \ IO as consoleReadLine;
        try {
            match console() {
                case Option.Some(c) =>
                    let line = consoleReadLine(c);
                    if (Object.isNull(line)) Err("EOF")
                    else Ok(line)
                case Option.None => Err("No JVM console")
            }
        } catch {
            case e: ##java.io.IOError =>
                import java.lang.Throwable.getMessage(): String;
                Err(getMessage(e))
        }
    }

    ///
    /// Converts `x` to a string and prints it to the console.
    ///
    /// Note: This function does not print to standard out.
    ///
    /// Warning: If no console is attached nothing is printed.
    ///
    /// Use `print` instead.
    ///
    /// See also `System/StdOut.print`.
    ///
    pub def print(x: a): Unit \ IO with ToString[a] =
        import java.io.Console.writer(): ##java.io.PrintWriter \ IO;
        import java.io.PrintWriter.print(String): Unit \ IO as printWriterPrint;
        console() |> Option.forEach(c -> printWriterPrint(writer(c), ToString.toString(x)))

    ///
    /// Converts `x` to a string and prints it to the console followed by a new line.
    ///
    /// Note: This function does not print to standard out.
    ///
    /// Warning: If no console is attached nothing is printed.
    ///
    /// Use `print` instead.
    ///
    /// See also `System/StdOut.println`.
    ///
    pub def printLine(x: a): Unit \ IO with ToString[a] =
        import java.io.Console.writer(): ##java.io.PrintWriter \ IO;
        import java.io.PrintWriter.println(String): Unit \ IO as printWriterPrintln;
        console() |> Option.forEach(c -> printWriterPrintln(writer(c), ToString.toString(x)))

    ///
    /// Flushes the console output.
    ///
    /// Note: This function does not flush standard out.
    ///
    /// Warning: If no console is attached nothing happens.
    ///
    /// See also `System/StdOut.flush`.
    ///
    pub def flush(): Unit \ IO =
        import java.io.Console.flush(): Unit \ IO as consoleFlush;
        console() |> Option.forEach(consoleFlush)

    ///
    /// Returns `s` formatted as bold.
    ///
    pub def bold(s: a): String with ToString[a] =
        let esc = escape();
        match colorSupport() {
            case ColorSupport.None      => ToString.toString(s)
            case ColorSupport.Ansi      =>
                esc + "[1m" + ToString.toString(s) + esc + "[0m"
            case ColorSupport.TrueColor =>
                esc + "[38;2;1m" + ToString.toString(s) + esc + "[0m"
        }

    ///
    /// Returns `s` formatted with color `hexCode`
    /// or if there's an error, returns the original string.
    /// Expected format of `hexCode`: "#" + "--" + "--" + "--"
    /// where each -- is a hex number between 00 and FF.
    ///
    pub def hex(hexCode: String, s: a): String with ToString[a] =
        let text = ToString.toString(s);
        let hexOpt: Option[String] = {
            use Option.flatMap;
            let* hex = String.stripPrefix(substr = "#", hexCode);
            if (String.length(hex) == 6) {
                let* r = String.sliceLeft(2, hex) |> stringToHex;
                let* g = String.slice(2, 4, hex) |> stringToHex;
                let* b = String.slice(4, 6, hex) |> stringToHex;
                Some(rgb((r, g, b), text, isFg = true))
            } else Option.None
        };
        hexOpt |> Option.getWithDefault(text)

    ///
    /// Retrieves the console of the JVM if there is one, otherwise None.
    ///
    def console(): Option[##java.io.Console] \ IO =
        import static java.lang.System.console(): ##java.io.Console \ IO as getConsole;
        getConsole() |> Object.toOption

    ///
    /// Helper for `hex`.
    /// Returns `hexNo` as a hex number.
    ///
    def stringToHex(hexNo: String): Option[Int32] \ {} =
        import static java.lang.Integer.parseInt(String, Int32): Int32 \ {};
        try {
            Some(parseInt(hexNo, 16))
        } catch {
            case _: ##java.lang.NumberFormatException =>
                Option.None
        }

    ///
    /// Returns the string `s` formatted with the given RGB color `c`.
    ///
    pub def rgb(c: (Int32, Int32 , Int32), s: String, opts: {isFg = Bool}): String =
        match colorSupport() {
            case ColorSupport.None      => s
            case ColorSupport.Ansi      =>
                let color = downsample(c, opts.isFg);
                formatAnsi(color, s)
            case ColorSupport.TrueColor =>
                formatTrueColor(c, s, opts.isFg)
        }

    ///
    /// Returns the string `s` formatted with the default blue color.
    ///
    pub def blue(s: a): String with ToString[a] =
        rgb(Blue |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default cyan color.
    ///
    pub def cyan(s: a): String with ToString[a] =
        rgb(Cyan |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default green color.
    ///
    pub def green(s: a): String with ToString[a] =
        rgb(Green |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default magenta color.
    ///
    pub def magenta(s: a): String with ToString[a] =
        rgb(Magenta |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default red color.
    ///
    pub def red(s: a): String with ToString[a] =
        rgb(Red |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default yellow color.
    ///
    pub def yellow(s: a): String with ToString[a] =
        rgb(Yellow |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default white color.
    ///
    pub def white(s: a): String with ToString[a] =
        rgb(White |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default black color.
    ///
    pub def black(s: a): String with ToString[a] =
        rgb(Black |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright black color.
    ///
    pub def blackBright(s: a): String with ToString[a] =
        rgb(BrightBlack |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright red color.
    ///
    pub def redBright(s: a): String with ToString[a] =
        rgb(BrightRed |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright green color.
    ///
    pub def greenBright(s: a): String with ToString[a] =
        rgb(BrightGreen |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright yellow color.
    ///
    pub def yellowBright(s: a): String with ToString[a] =
        rgb(BrightYellow |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright blue color.
    ///
    pub def blueBright(s: a): String with ToString[a] =
        rgb(BrightBlue |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright magenta color.
    ///
    pub def magentaBright(s: a): String with ToString[a] =
        rgb(BrightMagenta |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright cyan color.
    ///
    pub def cyanBright(s: a): String with ToString[a] =
        rgb(BrightCyan |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` formatted with the default bright white color.
    ///
    pub def whiteBright(s: a): String with ToString[a] =
        rgb(BrightWhite |> toRGB, ToString.toString(s), isFg = true)

    ///
    /// Returns the string `s` with background color of the default black color.
    ///
    pub def bgBlack(s: a): String with ToString[a] =
        rgb(BackgroundBlack |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default red color.
    ///
    pub def bgRed(s: a): String with ToString[a] =
        rgb(BackgroundRed |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default green color.
    ///
    pub def bgGreen(s: a): String with ToString[a] =
        rgb(Ansi.BackgroundGreen |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default yellow color.
    ///
    pub def bgYellow(s: a): String with ToString[a] =
        rgb(BackgroundYellow |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default blue color.
    ///
    pub def bgBlue(s: a): String with ToString[a] =
        rgb(BackgroundBlue |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default magenta color.
    ///
    pub def bgMagenta(s: a): String with ToString[a] =
        rgb(BackgroundMagenta |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default cyan color.
    ///
    pub def bgCyan(s: a): String with ToString[a] =
        rgb(BackgroundCyan |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default white color.
    ///
    pub def bgWhite(s: a): String with ToString[a] =
        rgb(BackgroundWhite |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default black color.
    ///
    pub def bgBlackBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightBlack |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default red color.
    ///
    pub def bgRedBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightRed |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default green color.
    ///
    pub def bgGreenBright(s: a): String with ToString[a] =
        rgb(Ansi.BackgroundBrightGreen |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default yellow color.
    ///
    pub def bgYellowBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightYellow |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default blue color.
    ///
    pub def bgBlueBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightBlue |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default magenta color.
    ///
    pub def bgMagentaBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightMagenta |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default cyan color.
    ///
    pub def bgCyanBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightCyan |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Returns the string `s` with background color of the default white color.
    ///
    pub def bgWhiteBright(s: a): String with ToString[a] =
        rgb(BackgroundBrightWhite |> toRGB, ToString.toString(s), isFg = false)

    ///
    /// Ansi Colors.
    ///
    enum Ansi {
        case Black
        case Red
        case Green
        case Yellow
        case Blue
        case Magenta
        case Cyan
        case White
        case BrightBlack
        case BrightRed
        case BrightGreen
        case BrightYellow
        case BrightBlue
        case BrightMagenta
        case BrightCyan
        case BrightWhite
        case BackgroundBlack
        case BackgroundRed
        case BackgroundGreen
        case BackgroundYellow
        case BackgroundBlue
        case BackgroundMagenta
        case BackgroundCyan
        case BackgroundWhite
        case BackgroundBrightBlack
        case BackgroundBrightRed
        case BackgroundBrightGreen
        case BackgroundBrightYellow
        case BackgroundBrightBlue
        case BackgroundBrightMagenta
        case BackgroundBrightCyan
        case BackgroundBrightWhite
    }

    ///
    /// Returns the foreground color of the given color `c`.
    ///
    def toAnsi(c: Ansi): Int32 =
        match c {
            case Ansi.Black                     => 30
            case Ansi.Red                       => 31
            case Ansi.Green                     => 32
            case Ansi.Yellow                    => 33
            case Ansi.Blue                      => 34
            case Ansi.Magenta                   => 35
            case Ansi.Cyan                      => 36
            case Ansi.White                     => 37
            case Ansi.BrightBlack               => 90
            case Ansi.BrightRed                 => 91
            case Ansi.BrightGreen               => 92
            case Ansi.BrightYellow              => 93
            case Ansi.BrightBlue                => 94
            case Ansi.BrightMagenta             => 95
            case Ansi.BrightCyan                => 96
            case Ansi.BrightWhite               => 97
            case Ansi.BackgroundBlack           => 40
            case Ansi.BackgroundRed             => 41
            case Ansi.BackgroundGreen           => 42
            case Ansi.BackgroundYellow          => 43
            case Ansi.BackgroundBlue            => 44
            case Ansi.BackgroundMagenta         => 45
            case Ansi.BackgroundCyan            => 46
            case Ansi.BackgroundWhite           => 47
            case Ansi.BackgroundBrightBlack     => 100
            case Ansi.BackgroundBrightRed       => 101
            case Ansi.BackgroundBrightGreen     => 102
            case Ansi.BackgroundBrightYellow    => 103
            case Ansi.BackgroundBrightBlue      => 104
            case Ansi.BackgroundBrightMagenta   => 105
            case Ansi.BackgroundBrightCyan      => 106
            case Ansi.BackgroundBrightWhite     => 107
        }

    ///
    /// Returns the RGB-triplet for the given color `c`.
    ///
    /// Based on the Ubuntu settings.
    ///
    /// See https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
    ///
    def toRGB(c: Ansi): (Int32, Int32, Int32) =
        match c {
            case Ansi.Black                     => (  1,   1,   1)
            case Ansi.Red                       => (222,  56,  43)
            case Ansi.Green                     => ( 57, 181,  74)
            case Ansi.Yellow                    => (255, 199,   6)
            case Ansi.Blue                      => (  0, 111, 184)
            case Ansi.Magenta                   => (118,  38, 113)
            case Ansi.Cyan                      => ( 44, 181, 233)
            case Ansi.White                     => (204, 204, 204)
            case Ansi.BrightBlack               => (128, 128, 128)
            case Ansi.BrightRed                 => (255,   0,   0)
            case Ansi.BrightGreen               => (  0, 255,   0)
            case Ansi.BrightYellow              => (255, 255,   0)
            case Ansi.BrightBlue                => (  0,   0, 255)
            case Ansi.BrightMagenta             => (255,   0, 255)
            case Ansi.BrightCyan                => (  0, 255, 255)
            case Ansi.BrightWhite               => (255, 255, 255)
            case Ansi.BackgroundBlack           => (  1,   1,   1)
            case Ansi.BackgroundRed             => (222,  56,  43)
            case Ansi.BackgroundGreen           => ( 57, 181,  74)
            case Ansi.BackgroundYellow          => (255, 199,   6)
            case Ansi.BackgroundBlue            => (  0, 111, 184)
            case Ansi.BackgroundMagenta         => (118,  38, 113)
            case Ansi.BackgroundCyan            => ( 44, 181, 233)
            case Ansi.BackgroundWhite           => (204, 204, 204)
            case Ansi.BackgroundBrightBlack     => (128, 128, 128)
            case Ansi.BackgroundBrightRed       => (255,   0,   0)
            case Ansi.BackgroundBrightGreen     => (  0, 255,   0)
            case Ansi.BackgroundBrightYellow    => (255, 255,   0)
            case Ansi.BackgroundBrightBlue      => (  0,   0, 255)
            case Ansi.BackgroundBrightMagenta   => (255,   0, 255)
            case Ansi.BackgroundBrightCyan      => (  0, 255, 255)
            case Ansi.BackgroundBrightWhite     => (255, 255, 255)
        }

    ///
    /// Down sample the given color `c` to an ANSI color code.
    ///
    def downsample(c: (Int32, Int32, Int32), isFg: Bool): Int32 =
        let candidates = {
            if(isFg) {
                Ansi.Black :: Ansi.Red :: Ansi.Green :: Ansi.Yellow :: Ansi.Blue :: Ansi.Magenta ::
                    Ansi.Cyan :: Ansi.White :: Ansi.BrightBlack :: Ansi.BrightRed :: Ansi.BrightGreen ::
                    Ansi.BrightYellow :: Ansi.BrightBlue :: Ansi.BrightMagenta :: Ansi.BrightCyan ::
                    Ansi.BrightWhite :: Nil }
            else {
                Ansi.BackgroundBlack :: Ansi.BackgroundRed :: Ansi.BackgroundGreen ::
                    Ansi.BackgroundYellow :: Ansi.BackgroundBlue :: Ansi.BackgroundMagenta ::
                    Ansi.BackgroundCyan :: Ansi.BackgroundWhite :: Ansi.BackgroundBrightBlack ::
                    Ansi.BackgroundBrightRed :: Ansi.BackgroundBrightGreen :: Ansi.BackgroundBrightYellow ::
                    Ansi.BackgroundBrightBlue :: Ansi.BackgroundBrightMagenta :: Ansi.BackgroundBrightCyan ::
                    Ansi.BackgroundBrightWhite :: Nil}
            };
        nearest(c, Ansi.BackgroundBlack, Int32.maxValue(), candidates) |> toAnsi

    ///
    /// Returns the ANSI color nearest to the given `target` color from the given list of `candidates`.
    ///
    def nearest(target: (Int32, Int32, Int32), bestCandidate: Ansi, bestDistance: Int32, candidates: List[Ansi]): Ansi =
        match candidates {
            case Nil     => bestCandidate
            case currentCandidate :: remainingCandidates =>
                let currentDistance = distanceSquared(target, toRGB(currentCandidate));
                if (currentDistance < bestDistance)
                    nearest(target, currentCandidate, currentDistance, remainingCandidates)
                else
                    nearest(target, bestCandidate, bestDistance, remainingCandidates)
        }

    ///
    /// Returns the square of the distance between the two colors `c1` and `c2`.
    ///
    def distanceSquared(c1: (Int32, Int32, Int32), c2: (Int32, Int32, Int32)): Int32 =
        let (r1, g1, b1) = c1;
        let (r2, g2, b2) = c2;
        (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)

    ///
    /// Returns the string `s` formatted with the given ANSI color `fg`.
    ///
    def formatAnsi(color: Int32, s: String): String =
        let esc = escape();
        esc + "[" + Int32.toString(color) + "m" + s + esc + "[0m"

    ///
    /// Returns the string `s` formatted with the given RGB color `c`.
    ///
    def formatTrueColor(c: (Int32, Int32, Int32), s: String, isFg: Bool): String =
        let (r, g, b) = c;
        let esc = escape();
        let red = Int32.toString(r);
        let gre = Int32.toString(g);
        let blu = Int32.toString(b);
        if (isFg) {
            esc + "[38;2;" + red + ";" + gre + ";" + blu + "m" + s + esc + "[0m"
        } else {
            esc + "[48;2;" + red + ";" + gre + ";" + blu + "m" + s + esc + "[0m"
        }

    ///
    /// Returns the escape character as a string.
    ///
    def escape(): String = Char.toString('\u001b')

    ///
    /// An enum that represents the colors supported by a terminal.
    ///
    enum ColorSupport {
        ///
        /// The terminal does not support any colors.
        ///
        case None,

        ///
        /// The terminal supports ANSI colors.
        ///
        case Ansi,

        ///
        /// The terminal supports 24-bit colors.
        ///
        case TrueColor
    }

    ///
    /// Returns the color support of the current terminal.
    ///
    def colorSupport(): ColorSupport =
        if (isTrueColorTerminal() or isWindowsTerminal())
            ColorSupport.TrueColor
        else if (isAnsiTerminal())
            ColorSupport.Ansi
        else
            ColorSupport.None

    ///
    /// Returns `true` if the terminal supports ANSI colors.
    ///
    def isAnsiTerminal(): Bool =
        Environment.getVar("TERM") |>
        Option.exists(p ->
            let keywords = "256" :: "ansi" :: "xterm" :: "screen" :: Nil;
            List.exists(s -> String.contains({substr = s}, p), keywords)
        )

    ///
    /// Returns `true` if the terminal supports 24-bit colors.
    ///
    def isTrueColorTerminal(): Bool =
        Environment.getVar("COLORTERM") |>
        Option.exists(p -> p == "truecolor")

    ///
    /// Returns `true` if the terminal is the Windows Terminal.
    ///
    def isWindowsTerminal(): Bool =
        not (Environment.getVar("WT_SESSION") |> Option.isEmpty)

}
