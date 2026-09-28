import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Takes a given input text and generates a tag cloud as an HTML file.
 *
 * @author Lucas Blauser
 * @author Branyan Rowland
 *
 */
public final class TagCloudGeneratorJCF {

    /**
     * No argument constructor--private to prevent instantiation.
     */
    private TagCloudGeneratorJCF() {
    }

    /**
     * Main method.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        // Open keyboard input.
        Scanner in = new Scanner(System.in);

        System.out.print("Enter input file name: ");
        String inFileName = in.nextLine();

        System.out.print("Enter output file name: ");
        String outFileName = in.nextLine();

        System.out.print("Enter # of words to be included in the tag cloud: ");
        String str = in.nextLine();
        // checks if the input contains any non-numeric characters.
        assert str.matches("\\d+") : "ERROR: Must input a numeric value";
        int numberOfWords = Integer.parseInt(str);
        assert numberOfWords > 0 : "ERROR: Tag cloud must include at least one word";

        in.close();

        // Open output
        PrintWriter fileOut;
        try {
            fileOut = new PrintWriter(new BufferedWriter(new FileWriter(outFileName)));
        } catch (IOException e) {
            System.err.println("ERROR: File not found");
            return;
        }

        // Open input
        BufferedReader fileIn;
        try {
            fileIn = new BufferedReader(new FileReader(inFileName));
        } catch (IOException e) {
            System.err.println("ERROR: File not found");
            fileOut.close();
            return;
        }

        outputHeader(fileOut, inFileName, numberOfWords);

        // Sequence stores each occurrence of each word from the input text.
        java.util.Queue<String> words = new LinkedList<String>();
        processInput(fileIn, words);

        // The keys of this map are the words that appear in Sequence "words" and the
        // and the values are the number of times each key appears.
        java.util.Map<String, Integer> wordCounts = new HashMap<>();
        countWords(words, wordCounts);

        // Reduces wordCounts to the desired number of words based on highest word count.
        trimMap(wordCounts, numberOfWords);

        // Calculates the font size a word should have based on its frequency.
        java.util.Map<Integer, Integer> fontSizes = generateFontSizes(wordCounts);

        // Comparator uses alphabetical order but ignores capitalization.
        Comparator<String> alphabeticalOrder = String.CASE_INSENSITIVE_ORDER;

        // List that stores each word in alphabetical order.
        java.util.List<String> alphabeticalList = new ArrayList<>();
        for (String s : wordCounts.keySet()) {
            alphabeticalList.add(s);
        }
        alphabeticalList.sort(alphabeticalOrder);

        outputTagCloud(fileOut, alphabeticalList, wordCounts, fontSizes);

        System.out.println("Tag cloud generated!");

        outputFooter(fileOut);

        //Close streams
        fileOut.close();
        try {
            fileIn.close();
        } catch (IOException e) {
            System.err.println("ERROR closing input file");
        }
    }

    /**
     * Outputs the heading information for the output HTML file.
     *
     * @param out
     *            stream to the output file.
     * @param inputFile
     *            user provided name of the input file.
     * @param numberOfWords
     *            number of words to be included in the tag cloud.
     * @requires out.isOpen
     * @ensures fileName.html contains properly formatted heading tags.
     */
    public static void outputHeader(PrintWriter out, String inputFile,
            int numberOfWords) {

        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println(
                "<title>Top " + numberOfWords + " words in " + inputFile + "</title>");

        out.println("<link href=\"https://cse22x1.engineering.osu.edu/2231/web-sw2/"
                + "assignments/projects/tag-cloud-generator/data/tagcloud.css\" "
                + "rel=\"stylesheet\" type=\"text/css\">");

        out.println("<link href=\"tagcloud.css\" rel=\"stylesheet\" type=\"text/css\">");
        out.println("</head>");
        out.println("<body>");
        out.println("<h2>Top " + numberOfWords + " words in " + inputFile + "</h2>");
        out.println("<hr>");
        out.println("<div class=\"cdiv\">");
        out.println("<p class=\"cbox\">");
    }

    /**
     * Stores each word from a string of text in an input file as individual
     * elements of a queue.
     *
     * @param in
     *            stream from the input file.
     * @param words
     *            queue where each word is stored as a string.
     * @requires in is open
     * @ensures words contains every occurrence of each word in the input file.
     */
    public static void processInput(BufferedReader in, java.util.Queue<String> words) {
        try {
            String currentLine = in.readLine();
            while (currentLine != null) {

                currentLine = currentLine.trim(); // Removes leading/trailing spaces.

                // Removes any characters that aren't letters, spaces or apostrophes
                currentLine = currentLine.replaceAll("[^A-Za-z' ]", "");

                // Turns the current line into an array of individual words
                String[] currentWords = currentLine.split("\\s+");

                for (String s : currentWords) {
                    if (!s.equals(" ") && s.length() > 0) {
                        words.add(s);
                    }
                }
                currentLine = in.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error reading input file");
        }

    }

    /**
     * Counts the number of times a word is contained in a sequence, and stores
     * the count in a map.
     *
     * @param words
     *            queue of words to be counted.
     * @param wordCounts
     *            map to store word counts.
     * @ensures wordCounts contains a key for each word found in words, and a
     *          value corresponding to the number of times each word appears.
     */
    public static void countWords(java.util.Queue<String> words,
            java.util.Map<String, Integer> wordCounts) {

        int queueSize = words.size();
        for (int i = 0; i < queueSize; i++) {
            String currentWord = words.remove();
            // If the word is not a duplicate, add it to the map
            if (!wordCounts.containsKey(currentWord)) {
                wordCounts.put(currentWord, 1);
            } else { // Increments word count if the map contains currentWord.
                int value = wordCounts.get(currentWord);
                value++;
                wordCounts.replace(currentWord, value);
            }
        }

    }

    /**
     * Trims a map so that it only includes n pairs with the highest values.
     *
     * @param wordCounts
     *            map of words and their counts.
     * @param n
     *            number of pairs to remain in the map.
     * @requires n <= |wordCounts|
     */
    public static void trimMap(java.util.Map<String, Integer> wordCounts, int n) {
        assert n <= wordCounts.size() : "Violation of : n < |wordCounts|";

        //Adds the Map Entry to a List so that it can be sorted using the List
        //sorting method
        List<Map.Entry<String, Integer>> entry = new ArrayList<>(wordCounts.entrySet());

        entry.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        wordCounts.clear();
        for (int i = 0; n > i; i++) {

            int count = entry.get(i).getValue();
            String word = entry.get(i).getKey();

            wordCounts.put(word, count);
            //Adds all the count and words back into the wordCounts Map
        }

    }

    /**
     * Creates a mapping between a words frequency in a text and its font size
     * using a logarithmic bucket sorting system.
     *
     * @param wordCounts
     *            map of words and their counts.
     * @return A map with word counts as keys and font sizes as values.
     * @requires |wordCounts| > 0
     */
    public static java.util.Map<Integer, Integer> generateFontSizes(
            java.util.Map<String, Integer> wordCounts) {
        assert wordCounts.size() > 0 : "Violation of : |wordCounts| > 0";

        java.util.Map<Integer, Integer> fontSizes = new HashMap<>();
        final int maxFontSize = 48;
        final int minFontSize = 10;
        int maxFreq = -1;
        int minFreq = -1;

        // Finding the highest and lowest word counts

        for (java.util.Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            int x = entry.getValue();
            // Gives maxFreq and minFreq valid initial values
            if (maxFreq == -1 && minFreq == -1) {
                maxFreq = x;
                minFreq = x;
            } else if (x > maxFreq) {
                maxFreq = x;
            } else if (x < minFreq) {
                minFreq = x;
            }
        }

        // log values used to calculate the ratio
        double logMin = Math.log(minFreq);
        double logMax = Math.log(maxFreq);

        // calculating each font size
        for (java.util.Map.Entry<String, Integer> entry : wordCounts.entrySet()) {

            // prevents unnecessary calculation and duplicate keys
            if (!fontSizes.containsKey(entry.getValue())) {
                int bucket = minFontSize;

                //prevents log(0)
                if (entry.getValue() > 0) {
                    double logFreq = Math.log(entry.getValue());

                    // logarithmic ratio between the current frequency and max frequency.
                    double ratio = (logFreq - logMin) / (logMax - logMin);
                    bucket = (int) Math
                            .round(minFontSize + ratio * (maxFontSize - minFontSize));
                }
                fontSizes.put(entry.getValue(), bucket);
            }
        }

        return fontSizes;
    }

    /**
     * Outputs a tag cloud given a set of words and the number of times those
     * words appear in a given text.
     *
     * @param out
     *            output writer.
     * @param wordList
     *            SortingMachine of words.
     * @param wordCounts
     *            Map to of words to word counts.
     * @param fontSizes
     *            Map of word counts to font sizes.
     * @requires out is open
     * @requires s is not in insertion mode
     * @ensures The HTML table contains all keys and values in wordCounts.
     */
    public static void outputTagCloud(PrintWriter out, java.util.List<String> wordList,
            java.util.Map<String, Integer> wordCounts,
            java.util.Map<Integer, Integer> fontSizes) {
        for (int i = 0; wordList.size() > i; i++) {
            String currentWord = wordList.get(i);
            // Sets currentWord to the String that is inside the List
            int currentCount = wordCounts.get(currentWord);
            //Sets the count to the same count that is found inside the map and is
            //the corresponding word to the count
            int fontSize = fontSizes.get(currentCount);
            //Sets the font size to the corresponding font size for the count
            out.println("<span style=\"cursor:default; font-size: " + fontSize
                    + "px;\" title=\"count: " + currentCount + "\">" + currentWord
                    + "</span>");
            // Prints all the information for each word
        }

    }

    /**
     * Outputs the "footer" with remaining closing tags for the output HTML
     * file.
     *
     * @param out
     *            SimpleWriter that writes to the output file.
     * @requires out.isOpen()
     * @ensures index.html contains proper closing tags
     */
    private static void outputFooter(PrintWriter out) {

        out.println("</p>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }

}
