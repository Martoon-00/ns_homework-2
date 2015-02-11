package com.company;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

@RunWith(Parameterized.class)
public class RandomTest extends ISolverTest {
    public RandomTest(double[][] A, double[] b) {
        super(A, b);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Random r = new Random();
        ArrayList<Object[]> params = new ArrayList<>();
        for (int t = 0; t < 10; t++) {
            int n = r.nextInt(defaultMaxSize) + 1;
            double[][] A = new double[n][n];
            for (int i = 0; i < n; i++) {
                A[i] = generateRandomVector(n);
            }
            double[] b = generateRandomVector(n);
            params.add(new Object[]{A, b});
        }
        return params;
    }

}