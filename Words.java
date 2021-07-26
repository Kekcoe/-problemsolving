package com.efimchick.ifmo.collections.countwords;

import java.util.*;

public class Words {

    public String countWords(List<String> lines) {
        List<String> wordList = new ArrayList<>();
        int count;

        for (String s : lines) {
            wordList.addAll(Arrays.asList(s.toLowerCase().split("[^A-Za-zА-Яа-я]+")));
        }

        Map<String, Integer> textMap = new HashMap<>();
        for (String s : wordList) {
            if (textMap.containsKey(s)) {
                count = textMap.get(s);
                textMap.put(s, count + 1);
            } else {
                textMap.put(s, 1);
            }
        }

        Iterator<Map.Entry<String, Integer>> iterMap = textMap.entrySet().iterator();
        while (iterMap.hasNext()) {
            Map.Entry<String, Integer> entry = iterMap.next();
            if (entry.getKey().length() < 4 || entry.getValue() < 10) {
                iterMap.remove();
            }
        }

        List<Map.Entry<String, Integer>> listSorted = new ArrayList<>(textMap.entrySet());
        listSorted.sort(new MapValueKeyComparator<String, Integer>());

        StringBuilder mapAsString = new StringBuilder();
        for (Map.Entry<String, Integer> entry : listSorted) {
            mapAsString.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
        }
        mapAsString.deleteCharAt(mapAsString.length() - 1);
        return mapAsString.toString();
    }
}