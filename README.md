# DuPAL Espresso Converter

## Introduction

The **DuPAL Analyzer** tool generates a JSON output representing the raw state changes registered during the analisys.

While that format contains the most information, it must be converted before tools like the `espresso` minimizer tool can be used to extract the logic equations from it. This tool is meant to convert the JSON file into one (or more, depending on the options passed) truth tables that can be minimized.

## Command line options

The command line supports the following parameters

```text
java -jar espresso_converter.jar <input_file> <output_file> [single output table] [use only source FIOs Y|N]
``` 

- **input_file**: *mandatory parameter*, path to the input JSON file
- **output_file**: *mandatory parameter*, path to the output table generated from the JSON file
- **single output table**: optional numeric parameter, range 0-X, where X is the total number of output functions for this PAL minus 1. If this is enabled, the destination file will contain a truth table with only the selected output.
- **use only source FIOs**: optional boolean parameter, either **Y** or **N**. If enabled, the output table will use only the values from the starting state as feedbacks for the output. See below.

### Asynchronous feedbacks

Some output equations have the output value itself as one of their term, for example:

```text
o11 = /o11 * i5
```

In this case, when generating a truth table, the input that define such outputs should be:

- The output (feedback) value itself from the starting state
- The feedbacks from the destination state
- The inputs fed into the equation

As the table would be different for each output, we can enable this only when generating a single-output table.

## Requirements

The tool must be invoked using a Java 1.8 compatible JRE.
