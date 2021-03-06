package com.company.lang;

import Jama.Matrix;
import com.company.conjugateGradient.DaiYuan;
import com.company.conjugateGradient.FletcherReeves;
import com.company.conjugateGradient.HestenesStiefel;
import com.company.conjugateGradient.PolakRibiere;
import com.company.gauss.GaussSolver;
import com.company.jacobi.Jacobi;
import com.company.jamaSolver.JamaSolver;
import com.company.seidel.Seidel;
import com.company.seidel.SeidelRelaxation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SolutionHandlers {

    /**
     * File with result tables.
     */
    public static final File outFile = new File("Results.xlsx");

    /**
     * Creates some set of solvers. May be used as parameter in methods below.
     */
    public static ISolver[] constructAllSolutions(Equation eq, double eps) {
        return new ISolver[]{
                new DaiYuan(eq.A, eq.b),
                new FletcherReeves(eq.A, eq.b),
                new HestenesStiefel(eq.A, eq.b),
                new PolakRibiere(eq.A, eq.b),
                new GaussSolver(eq.A, eq.b),
//                do not set iterationsNum to any occurred number here. In methods of this class, if solution reaches 10000 iterations,
//                  it would be considered hanged and measuring wouldn't be put to table - save this working.
                new Jacobi(eq.A, eq.b, eps, Integer.MAX_VALUE),
                new Seidel(eq.A, eq.b, eps, Integer.MAX_VALUE),
                new SeidelRelaxation(eq.A, eq.b, eps, Integer.MAX_VALUE, 0.3),
                new SeidelRelaxation(eq.A, eq.b, eps, Integer.MAX_VALUE, 1.8),
        };
    }

    /**
     * Prints to Result.xlsx solution of few of first solvers listed in {@code constructAllSolutions} and displays diagram which shows convergence speed.
     * In case of correct solution, solver's sequence of x will end with log10(eps) (check this on diagram).
     *
     * @param componentNum       number of printed component of solution vector.
     *                           If set to -1, norm is printed instead.
     * @param format             chooses template file. This influences on how much solutions / versions of x will be written
     * @param solversConstructor function-creator with signature Equation -> Eps -> ISolver[]
     */
    public static void showConvergence(Equation equation, double eps, int componentNum, TemplateFormat format, BiFunction<Equation, Double, ISolver[]> solversConstructor) throws IOException {
        if (componentNum < -1 || componentNum > equation.size())
            throw new RuntimeException("Wrong component requested");

//        some useful variables
        File inFile = new File(format.filePath);      // contains diagram, using it as basement

        ISolver[] solvers = solversConstructor.apply(equation, eps);   // initialize solvers
        int solversNum = Math.min(solvers.length, format.displayedSolversNum);           // number of solvers to display
        double[] etalon_x = new JamaSolver(equation.A, equation.b).solve();                                // exact solution

        Function<Matrix, Double> getComponent = componentNum == -1 ?                     // returns specified component of x / norm of x when componentNum == -1
                Matrix::normInf :                                                          // delegates to Matrix::norm   (componentNum == -1)
                (Matrix x) -> Math.abs(x.get(componentNum, 0));                            // returns specified component (componentNum >= 0)

//        lets write some data to file
        Workbook workbook;
//        read from template file
        try (InputStream in = new BufferedInputStream(new FileInputStream(inFile))) {
            workbook = new XSSFWorkbook(in);
        }
        Sheet sheet = workbook.getSheetAt(0);

//            fill table header with solvers' names
        Row firstRow = sheet.createRow(0);
        firstRow.createCell(0).setCellValue("x");
        for (int i = 0; i < solversNum; i++) {
            firstRow.createCell(i + 1).setCellValue(solvers[i].getName());
        }

//            insert values (into file copy which is stored into memory)
        for (int i = 0; i < solversNum; i++) {
            try {
                int rowNum = 1;
                for (double[] x : solvers[i]) {
                    // select next row
                    Row row;
                    // if row doesn't exist, first create it
                    if (sheet.getLastRowNum() < rowNum) {
                        row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(rowNum - 1);
                    } else {
                        row = sheet.getRow(rowNum++);
                    }

                    // fill cell
                    row.createCell(i + 1).setCellValue(Math.log10(Math.max(
                            getComponent.apply(new Matrix(x, x.length).minus(new Matrix(etalon_x, etalon_x.length))),
                            eps
                    )));

                    // stop when no more rows needed
                    if (rowNum > format.displayedRowsNum) break;
                }
            } catch (ISolver.SolverException ex) {
                System.out.println(solvers[i].getName() + " failed");
            }
        }

//        write to result file
        writeToResultFile(workbook);
    }

    /**
     * Prints to Results.xlsx number of iterations required to complete solution for input parameters of varied sizes.
     * When solution fails on some equation size or works for too much, data is not registered into table.
     *
     * @param equationProducer   function-creator with signature EquationSize -> Equation
     * @param inputMaxSize       max size of input data
     * @param eps                maximal precision which will be shown on diagram
     * @param inputExpGrowth     determines whether input data size should grow exponentially or linearly.
     *                           First case may be useful for comparing solutions, whereas second one provides graphic dependence of iteration number on input size
     * @param launchesNum        number of launching a single solution on single input data size, provides smoothing
     * @param solversConstructor function-creator with signature Equation -> Eps -> ISolver[]
     * @throws IOException
     */
    public static void showIterationsNum(EquationFactory equationProducer, int inputMaxSize, double eps, boolean inputExpGrowth, int launchesNum, BiFunction<Equation, Double, ISolver[]> solversConstructor) throws IOException {
//        some usefull variables
        TemplateFormat format = TemplateFormat.CountIterations;
        File inFile = new File(format.filePath);      // contains diagram, using it as basement

        ISolver[] uselessSolvers = solversConstructor.apply(equationProducer.apply(1), eps);
        int solversNum = Math.min(uselessSolvers.length, format.displayedSolversNum);           // number of solvers to display
        int rowsNum = Math.min(inputMaxSize, format.displayedRowsNum);

//        lets write some data to file
        Workbook workbook;
//        read from template file
        try (InputStream in = new BufferedInputStream(new FileInputStream(inFile))) {
            workbook = new XSSFWorkbook(in);
        }
        Sheet sheet = workbook.getSheetAt(0);

//        fill table header with solvers' names
        Row firstRow = sheet.createRow(0);
        firstRow.createCell(0).setCellValue("n \\ solver");
        for (int i = 0; i < solversNum; i++) {
            firstRow.createCell(i + 1).setCellValue(uselessSolvers[i].getName());
        }

//            insert values (into file copy which is stored into memory)
        for (int i = 0; i < rowsNum; i++) {
            int n = inputExpGrowth ?
                    (int) Math.pow(10, (double) i / (rowsNum - 1) * Math.log10(inputMaxSize)) :
                    (int) ((double) i / (rowsNum - 1) * (inputMaxSize - 1)) + 1;

//            launch solution on various inputs
            int[] result = new int[uselessSolvers.length];
            for (int j = 0; j < launchesNum; j++) {
                int solverNum = 0;
                for (ISolver solver : solversConstructor.apply(equationProducer.apply(n), eps)) {
//                    skip if has failed at previous launch
                    if (result[solverNum] < 0) continue;

//                    count iterations, if too many or exception thrown then result = MIN_VALUE
                    try {
                        int k = 0;
                        for (double[] x : solver) {
                            if (k++ > 10000) {
                                result[solverNum] = Integer.MIN_VALUE;
                                break;
                            }
                        }
                        result[solverNum++] += k;
                    } catch (Exception e) {
                        result[solverNum] = Integer.MIN_VALUE;
                    }
                }
            }

//            put result of launches to table
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(n);
            for (int j = 0; j < result.length; j++) {
                if (result[j] >= 0)
                    row.createCell(j + 1).setCellValue((double) result[j] / launchesNum);
            }
        }

//        write to result file
        writeToResultFile(workbook);
    }

    /**
     * Prints workbook to result file. If failed, prints warning to console and tries again after enter key pressed.
     */
    private static void writeToResultFile(Workbook workbook) throws IOException {
        try {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                workbook.write(out);
            }
        } catch (IOException e) {
            System.err.println("Error while accessing file:");
            System.err.println("\t" + e.getMessage());
            System.err.println("Press enter to retry...");
            System.in.read();
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                workbook.write(out);
            }
        }
    }

    /*
    public static void writeSolve(double[][] A, double[] b) {
        String FILE_SOLVERS_NAME = "Result.xls";
        double eps = 1e-10;
        int maxIteration = (int) 10e2;

        ISolver[] solvers = SolutionHandlers.constructAllSolutions(A, b, eps, maxIteration);

        try (FileOutputStream out = new FileOutputStream(FILE_SOLVERS_NAME)) {
            Workbook workbook = new HSSFWorkbook();
            for (ISolver solver : solvers)
                workbook.createSheet(solver.getName());
            for (int i = 0, size = solvers.length; i < size; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                ISolver solver = solvers[i];
//                if (solver instanceof ISolverIterative) {
//		           TODO Write All x from iterable methods
//				}
                double[] doubles = solver.solve();
                for (int j = 0, l = doubles.length; j < l; j++)
                    sheet.createRow(j).createCell(0).setCellValue(doubles[j]);
            }

            workbook.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
      */

}
