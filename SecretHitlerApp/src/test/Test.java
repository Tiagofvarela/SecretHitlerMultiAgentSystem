package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Test {

    public static void main(String[] args) {

        List<Integer> abc = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));

        List<Integer> generated = generateValues(abc);

        System.out.println("ABC:");
        printList(abc);
        System.out.println("\nGenerated:");
        printList(generated);

    }

    private static void printList(List<Integer> abc) {
        System.out.print("[");
        for (Integer i :
                abc) {
            System.out.print(", " + i);
        }
        System.out.print("]");
    }

    private static List<Integer> generateValues(List<Integer> abc) {
        Random rnd = new Random();
        List<Integer> fascistsPlayers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int nextFascist = rnd.nextInt(abc.size());
            fascistsPlayers.add(abc.remove(nextFascist));
        }
        return fascistsPlayers;
    }
}
