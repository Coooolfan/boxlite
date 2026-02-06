package io.boxlite.samples.codebox.basic;

import io.boxlite.highlevel.CodeBox;
import io.boxlite.highlevel.ExecOutput;

/** Basic CodeBox sample: execute Python code and print structured result. */
public final class CodeBoxBasicApp {
    private CodeBoxBasicApp() {
    }

    public static void main(String[] args) {
        try (CodeBox box = new CodeBox().start()) {
            ExecOutput output = box.run(
                "import platform\n" +
                "print('hello-from-codebox')\n" +
                "print(platform.python_version())\n"
            );

            System.out.println("Exit code: " + output.exitCode());
            System.out.println("Stdout:");
            System.out.println(output.stdout().strip());
            if (!output.stderr().isBlank()) {
                System.out.println("Stderr:");
                System.out.println(output.stderr().strip());
            }
            if (output.errorMessage() != null && !output.errorMessage().isBlank()) {
                System.out.println("Error message: " + output.errorMessage());
            }
        }
    }
}
