package examprep;

import java.util.function.Function;
import java.util.function.Predicate;

public class Mathtests {
    public static void main(String[] args) {


        Function<String, Float> floatify = Float::parseFloat;

        Float result = floatify.compose( ( String str ) -> str + ".25" )
                .andThen( num -> num * 2 )
                .apply( "2" );
        System.out.println( result );

        float pi = 3.141592653f;
        int zero= 0;

        System.out.println(pi/zero);

        Predicate<Integer> isZero = i -> i == 0;

//        long l = Long.MAX_VALUE;
//        int i = (int) l;
//        System.out.println("result:" + i);
//
//        int j = (int) Integer.MAX_VALUE + 5;
//        System.out.println("result:" + j);
//
//        String blockTest1 = """
//                try with normal enter
//                to see print
//                """;
//        String blockTest2 = """
//                then try with backshlash \
//                to see print
//                """;
//        System.out.println("result1:");
//        System.out.println(blockTest1);
//        System.out.println("result2:");
//        System.out.println(blockTest2);
//
//        String block3 = """
//                test
//                """;
//        System.out.println("result3:");
//        System.out.println(block3);
//        LocalDate date = LocalDate.of(1997, 2, 28).plusDays(10000);
//        System.out.println(date);
////        throw new IOException();
//        List<Long> cannesFestivalfeatureFilms = LongStream.range( 1, 1945 )
//                .boxed()
//                .toList();
//
//        try ( var executor = Executors.newVirtualThreadPerTaskExecutor() ) {
//            cannesFestivalfeatureFilms.stream()
//                    .limit( 25 )
//                    .forEach( film -> executor.submit( () -> {
//                        System.out.println( film );
//                    } ) );
//        }

//        Runnable task1 = () -> System.out.println("Executing Task-1");
//        Callable<String> task2 = () -> {
//            System.out.println("Executing Task-2");
//            return "Task-2 Finish.";
//        };
//
//        ExecutorService execService = Executors.newCachedThreadPool();
//        execService.execute(task1);
////        execService.call(task2);
//
//        execService.awaitTermination(3, TimeUnit.SECONDS);
//        execService.shutdownNow();
    }
}
