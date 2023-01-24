package org.glygen.gws2xlsx;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glygen.gws2xlsx.model.InputFile;
import org.glygen.gws2xlsx.model.JobObject;

public class App {
    
    public static void main(String[] args) {
        
     // parse the command line arguments and store them
        Options options = App.buildComandLineOptions();
        AppArguments arguments = App.processCommandlineArguments(args, options);
        if (arguments == null)
        {
            // error messages and command line options have been printed already
            return;
        }
        
        GlytoucanRegistryApp registry = new GlytoucanRegistryApp();
        if (arguments.getGwsFile() != null) {
            try {
                JobObject job = new JobObject();
                InputFile processed = registry.processSingleFile(arguments.getGwsFile(), arguments.glytoucanGeneration, arguments.getCartoonGeneration());
                job.addFile(processed);
                registry.writeIntoExcel(job, arguments.getOutputFolder());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (arguments.getInputFolder() != null) {
            JobObject job = registry.processInputFolder(arguments.getInputFolder(), arguments.getGlytoucanGeneration(), arguments.getCartoonGeneration());
            try {
                registry.writeIntoExcel(job, arguments.getOutputFolder());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (arguments.getJobFile() != null) {
            JobObject job = registry.processJobFile(arguments.getJobFile());
            try {
                registry.writeIntoExcel(job, arguments.getOutputFolder());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static AppArguments processCommandlineArguments(String[] args, Options options) {
     // initialize the arguments from command line
        AppArguments arguments = null;
        try
        {
            arguments = App.parseArguments(args, options);
            if (arguments == null)
            {
                // failed, message was printed, time to go
                App.printComandParameter(options);
                return null;
            }
        }
        catch (ParseException e)
        {
            System.out.println("Invalid commandline arguments: " + e.getMessage());
            App.printComandParameter(options);
            return null;
        }
        catch (Exception e)
        {
            System.out.println(
                    "There was an error processing the command line arguments: " + e.getMessage());
            App.printComandParameter(options);
            return null;
        }
        return arguments;
    }

    private static void printComandParameter(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "<command> -f <GWSFile> (or -d <GWSFolder> or -j <JobFile>) -o <OutputFolder> -g (or -c)",
                options);
        
    }

    private static AppArguments parseArguments(String[] args, Options options) throws ParseException {
     // create the command line parser
        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        CommandLine commandLine = parser.parse(options, args);
        AppArguments arguments = new AppArguments();
        // overwrite from arguments
        arguments.setGwsFile(commandLine.getOptionValue("f"));
        arguments.setOutputFolder(commandLine.getOptionValue("o"));
        arguments.setInputFolder(commandLine.getOptionValue("d"));
        arguments.setJobFile(commandLine.getOptionValue("j"));
        arguments.setGlytoucanGeneration(commandLine.hasOption("g"));
        arguments.setCartoonGeneration(commandLine.hasOption("c"));
        // check settings
        if (!App.checkArguments(arguments))
        {
            return null;
        }
        return arguments;
    }

    private static boolean checkArguments(AppArguments arguments) {
        boolean t_valid = true;
        // input file
        if (arguments.getGwsFile() != null)
        {
            // input file must exist
            File t_file = new File(arguments.getGwsFile());
            if (t_file.exists())
            {
                if (t_file.isDirectory())
                {
                    System.out.println("GWS file (-f) can not be a directory.");
                    t_valid = false;
                }
            }
            else
            {
                System.out.println("GWS file (-f) does not exist.");
                t_valid = false;
            }
        }
        else if (arguments.getInputFolder() != null) {
            // input folder must exist
            File t_file = new File(arguments.getInputFolder());
            if (t_file.exists())
            {
                if (!t_file.isDirectory())
                {
                    System.out.println("Input folder (-d) must be a directory.");
                    t_valid = false;
                }
            }
            else
            {
                System.out.println("Input folder (-d) does not exist.");
                t_valid = false;
            }   
        } else if (arguments.getJobFile() != null) {
            // job file must exist
            File t_file = new File(arguments.getJobFile());
            if (t_file.exists())
            {
                if (t_file.isDirectory())
                {
                    System.out.println("Job file (-j) can not be a directory.");
                    t_valid = false;
                }
            }
            else
            {
                System.out.println("Job file (-j) does not exist.");
                t_valid = false;
            }
        }
        else {
            System.out.println("One of GWS file (-f) or input directory (-d) or previous job file (-j) should be provided!");
            t_valid = false;
        }
        
        // output folder
        if (arguments.getOutputFolder() != null)
        {
            File t_file = new File(arguments.getOutputFolder());
            if (!t_file.exists())
            {
                if (!t_file.mkdirs())
                {
                    System.out.println("Unable to create output folder.");
                    t_valid = false;
                }
            }
        }
        else
        {
            System.out.println("Output folder (-o) is missing.");
            t_valid = false;
        }
        
        if (arguments.getCartoonGeneration() == null && arguments.getGlytoucanGeneration() == null) {
            System.out.println("At least one of GlyTouCan ID generation (-g) or Cartoon generation (-c) should be provided!");
            t_valid = false;
        }
        return t_valid;
    }

    private static Options buildComandLineOptions() {
     // create the Options
        Options t_options = new Options();
        // configuration file
        Option t_option = new Option("f", "file", true,
                "GWS file");
        t_option.setArgs(1);
        t_option.setRequired(false);
        t_options.addOption(t_option);
        // output folder
        t_option = new Option("d", "directory", true,
                "Folder with GWS files.");
        t_option.setArgs(1);
        t_option.setRequired(false);
        t_options.addOption(t_option);
        // properties file
        t_option = new Option("g", "glytoucanid", false,
                "GlyTouCan ID generation.");
        t_option.setArgs(0);
        t_option.setRequired(false);
        t_options.addOption(t_option);
        // mapping folder
        t_option = new Option("c", "cartoon", false, "Cartoon generation.");
        t_option.setArgs(0);
        t_option.setRequired(false);
        t_options.addOption(t_option);
        // writing geneless
        t_option = new Option("o", "output", true, "Output folder that will contain the excel sheet.");
        t_option.setArgs(1);
        t_option.setRequired(true);
        t_options.addOption(t_option);
        
        t_option = new Option("j", "job", true, "Run using previous job.");
        t_option.setArgs(1);
        t_option.setRequired(false);
        t_options.addOption(t_option);
        return t_options;
    }
    
    
}
