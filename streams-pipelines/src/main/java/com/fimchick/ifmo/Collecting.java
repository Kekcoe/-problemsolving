package com.efimchick.ifmo;

import com.efimchick.ifmo.util.CourseResult;
import com.efimchick.ifmo.util.Person;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Collecting {

    public int sum(IntStream intStream){
        return intStream.sum();
    }

    public int production(IntStream intStream) {
        return intStream.reduce(1, (a,b) -> a * b);
    }

    public int oddSum(IntStream intStream){
        return intStream.filter(i -> i % 2 != 0).sum();
    }

    public Map sumByRemainder(int a, IntStream intStream){
        return  intStream.boxed()
                .collect(Collectors.groupingBy(x -> x % a, Collectors.summingInt(Integer::intValue)));
    }

    private final Set<String> allHistoryTasks = Set.of("Phalanxing", "Shieldwalling", "Tercioing", "Wedging");
    private final String[] historyTasks = allHistoryTasks.toArray(new String[0]);


    private Map<Person, Map<String, Integer>> addHistoryIfPresent(Stream<CourseResult> stream) {
        return stream.collect(Collectors.toMap(
                CourseResult::getPerson,
                (x) -> {
                    if (allHistoryTasks.containsAll(x.getTaskResults().keySet()))
                        IntStream.range(0, allHistoryTasks.size())
                                .parallel()
                                .forEach(i -> x.getTaskResults().putIfAbsent(historyTasks[i], 0));
                    return x.getTaskResults();
                }));
    }

    public Map<Person, Double> totalScores(Stream<CourseResult> stream) {
        return addHistoryIfPresent(stream)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        x -> x.getValue()
                                .values()
                                .stream()
                                .mapToDouble(Integer::doubleValue)
                                .average()
                                .getAsDouble()));
    }

    public double averageTotalScore(Stream<CourseResult> stream) {
        return addHistoryIfPresent(stream)
                .values()
                .stream()
                .flatMap(x -> x.values().stream())
                .collect(Collectors.toList())
                .stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .getAsDouble();
    }

    public Map<String, Double> averageScoresPerTask(Stream<CourseResult> stream){
        List<Map<String, Integer>> list = stream
                .map(CourseResult::getTaskResults)
                .collect(Collectors.toList());

        return list
                .stream()
                .flatMap(e->e.entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingDouble(Map.Entry::getValue))
                ).entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e->e.getValue() / list.size()
                        )
                );

    }

    public Map<Person, String> defineMarks(Stream<CourseResult> stream){
        return addHistoryIfPresent(stream)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (x) -> {
                            double m = x.getValue()
                                    .values()
                                    .stream()
                                    .mapToDouble(Integer::doubleValue)
                                    .average()
                                    .getAsDouble();
                            return defineMark(m);
                        }
                ));
    }

    public String easiestTask(Stream<CourseResult> stream){
        return averageScoresPerTask(stream)
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    public Collector<CourseResult, Set<CourseResult>, String> printableStringCollector(){
        return new Collector<CourseResult, Set<CourseResult>, String>() { // anonymous class implements Collector
            @Override
            public Supplier<Set<CourseResult>> supplier() {
                return HashSet::new;
            }

            @Override
            public BiConsumer<Set<CourseResult>, CourseResult> accumulator() {
                return Set::add;
            }

            @Override
            public BinaryOperator<Set<CourseResult>> combiner() {
                return
                        (l, r) -> {
                            l.addAll(r);
                            return l;
                        };
            }

            @Override
            public Function<Set<CourseResult>, String> finisher() {
                return s->{

                    Map<Person, Map<String, Integer>> map = addHistoryIfPresent(s.stream());

                    List<String> tasksList = map.values()
                            .stream()
                            .flatMap(e -> e.keySet()
                                    .stream())
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());

                    List<String> studentsList = new ArrayList<>(getMapMarks(s).keySet());

                    int maxLengthStudent = Collections.max(studentsList, (Comparator.comparingInt(String::length))).length();

                    String header = header(tasksList, maxLengthStudent);
                    String students = studentList(s, tasksList, maxLengthStudent);
                    String average = footer(s, tasksList, maxLengthStudent);

                    return header + students + average;
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }

            private String header(List<String> tasksList, int maxLen){
                StringBuilder format = new StringBuilder("%-" + maxLen + "s | ");
                String[] args = new String[tasksList.size()+3];
                args[0] = "Student";
                args[args.length-2] = "Total";
                args[args.length-1] = "Mark";
                for(int i=0; i<tasksList.size(); i++){
                    String task = tasksList.get(i);
                    args[i+1] = task;
                    format.append("%").append(task.length()).append("s | ");
                }
                format.append("%5s | ").append("%4s |\n");

                return String.format(format.toString(), (Object[]) args);
            }

            private String studentList(Set<CourseResult> s, List<String> tasksList, int maxLen){

                Map<String, Integer[]> mapMarks = new TreeMap<>(getMapMarks(s));

                List<String> studentsList = new ArrayList<>(mapMarks.keySet());
                List<Integer[]> studentsMarks = new ArrayList<>(mapMarks.values());

                List<Double> totalScores = getTotalScores(s);

                List<String> marksList = getAverageMarks(s);

                List<String> listArgs = new ArrayList<>();
                String[] args = new String[tasksList.size()+3];
                StringBuilder format = new StringBuilder();
                for(int i=0; i<studentsList.size(); i++){
                    args[0] = studentsList.get(i);
                    args[args.length-2] = String.format("%.2f", totalScores.get(i)).replace(",", ".");
                    args[args.length-1] = marksList.get(i);
                    format.append("%-").append(maxLen).append("s | ");
                    for(int j=0; j<studentsMarks.get(i).length; j++){
                        String task = tasksList.get(j);
                        Integer[] marks = studentsMarks.get(i);
                        args[j+1] = Integer.toString(marks[j]);
                        format.append("%").append(task.length()).append("s | ");
                    }
                    format.append("%5s | ").append("%4s |\n");
                    listArgs.addAll(Arrays.asList(args));
                }
                return String.format(format.toString(), listArgs.toArray());
            }


            private String footer(Set<CourseResult> s, List<String> tasksList,  int maxLen){

                List<Double> averagePerTask = new ArrayList<>(new TreeMap<>(averageScoresPerTask(s.stream())).values());

                double average = getTotalScores(s)
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .getAsDouble();

                String mark = defineMark(average);

                StringBuilder format = new StringBuilder("%-" + maxLen + "s | ");
                String[] args = new String[averagePerTask.size()+3];
                args[0] = "Average";
                args[args.length-2] = String.format("%.2f", average).replace(',', '.');
                args[args.length-1] = mark;

                for(int i=0; i<averagePerTask.size(); i++){
                    args[i+1] = String.format("%.2f",averagePerTask.get(i)).replace(",", ".");
                    format.append("%").append(tasksList.get(i).length()).append("s | ");
                }
                format.append("%5s | ").append("%4s |");
                return String.format(format.toString(), (Object[]) args);
            }


            private List<Double> getTotalScores(Set<CourseResult> s){
                Map<String, Double> map = totalScores(s.stream())
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                x->(x.getKey().getLastName() + " " + x.getKey().getFirstName()),
                                Map.Entry::getValue
                        ));
                return new ArrayList<>(new TreeMap<>(map).values());
            }

            private List<String> getAverageMarks(Set<CourseResult> s){
                Map<String, String> map = defineMarks(s.stream())
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                x->(x.getKey().getLastName() + " " + x.getKey().getFirstName()),
                                Map.Entry::getValue
                        ));
                return new ArrayList<>(new TreeMap<>(map).values());
            }

            private Map<String, Integer[]> getMapMarks(Set<CourseResult> s){
                return s.stream()
                        .collect(Collectors.toMap(
                                x->x.getPerson().getLastName() + " " + x.getPerson().getFirstName(),
                                x-> new ArrayList<>(x.getTaskResults()
                                        .entrySet())
                                        .stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .map(Map.Entry::getValue)
                                        .collect(Collectors.toList()).toArray(new Integer[0])
                        ));
            }
        };
    }

    private String defineMark (double mark){
        if(mark > 90 && mark <=100)
            return "A";
        else if (mark>=83 && mark <=90)
            return "B";
        else if (mark>=75 && mark <83)
            return "C";
        else if (mark>=68 && mark<75)
            return "D";
        else if (mark>=60 && mark<68)
            return "E";
        else
            return "F";
    }

    public static void main(String[] args) {

    }

}
