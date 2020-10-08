# DuPAL Espresso Converter

## Introduction

The **DuPAL Analyzer** tool generates a JSON output representing the raw state changes registered during the analisys.

While that format contains the most information, it must be converted before tools like the `espresso` minimizer tool can be used to extract the logic equations from it. This tool is meant to convert the JSON file into one (or more, depending on the type of PAL) truth tables that can be minimized.

In case the PAL has both normal and registered outputs, **two truth tables will be generated**: one with states for the normal outputs and the other for the registered outputs.

## Command line options

The command line supports the following parameters

```text
java -jar espresso_converter.jar <input_file> <output_file> [single output table] [use only source FIOs Y|N]
``` 

- **input_file**: *mandatory parameter*, path to the input JSON file
- **output_file**: *mandatory parameter*, path to the output table generated from the JSON file. In case the tool needs to generate multiple tables, it will do so by appending a suffix to the output file name.
- **single output table**: optional numeric parameter, range 0-X, where X is the total number of output functions for this PAL minus 1. If this is enabled, the tool will generate the table for the selected output only.
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

## Requirements

The tool must be invoked using a Java 1.8 compatible JRE.
