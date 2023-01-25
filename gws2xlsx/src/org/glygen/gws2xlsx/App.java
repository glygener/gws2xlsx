package org.glygen.gws2xlsx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glygen.gws2xlsx.model.InputFile;
import org.glygen.gws2xlsx.model.JobObject;
import org.glygen.gws2xlsx.util.GlytoucanUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

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
        
        Properties prop;
        try {
            prop = App.loadRegistryProperties();
            // settings for GlytoucanUtil
            GlytoucanUtil.getInstance().setApiKey(prop.getProperty("glytoucan_api_key"));
            GlytoucanUtil.getInstance().setUserId(prop.getProperty("glytoucan_user"));
        } catch (IOException e1) {
            System.err.println ("Error loading properties file " + e1.getMessage());
            System.exit(1);
        }
        
        GlytoucanRegistryApp registry = new GlytoucanRegistryApp();
        JobObject job = null;
        if (arguments.getGwsFile() != null) {
            try {
                job = new JobObject();
                InputFile processed = registry.processSingleFile(arguments.getGwsFile(), arguments.glytoucanGeneration, arguments.getCartoonGeneration());
                job.addFile(processed);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (arguments.getInputFolder() != null) {
            job = registry.processInputFolder(arguments.getInputFolder(), arguments.getGlytoucanGeneration(), arguments.getCartoonGeneration());
        } else if (arguments.getJobFile() != null) {
            try {
                job = registry.processJobFile(arguments.getJobFile(), arguments.getGlytoucanGeneration(), arguments.getCartoonGeneration());
            } catch (JsonProcessingException | FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        try {
            registry.saveJob (job, arguments.getOutputFolder() + File.separator + "job" + System.currentTimeMillis() + ".json");
            registry.writeIntoExcel(job, arguments.getOutputFolder());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        t_option = new Option("o", "output", true, "Output folder for the excel file and job files");
        t_option.setArgs(1);
        t_option.setRequired(true);
        t_options.addOption(t_option);
        
        t_option = new Option("j", "job", true, "Run using previous job.");
        t_option.setArgs(1);
        t_option.setRequired(false);
        t_options.addOption(t_option);
        return t_options;
    }
    
    /**
     * Load the input parameter from a properties file.
     *
     * @return Properties object with the values from the file
     * @throws Exception
     *             If the loading of the properties file fails
     */
    public static Properties loadRegistryProperties() throws IOException
    {
        // open the file
        FileReader t_reader = new FileReader("registry.properties");
        // read properties
        Properties t_properties = new Properties();
        t_properties.load(t_reader);
        // close file
        t_reader.close();
        return t_properties;
    }
    
    
}
