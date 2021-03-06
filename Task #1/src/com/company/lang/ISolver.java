package com.company.lang;

import Jama.Matrix;

import java.util.Arrays;
import java.util.Iterator;

public abstract class ISolver implements Iterable<double[]> {
	protected final Matrix A, b;

	public ISolver(double[][] A, double[] b) {
		this.A = new Matrix(A).copy();
		this.b = new Matrix(b, b.length).copy();

		// check given dimensions are correct
		if (
				this.A.getColumnDimension() != this.A.getRowDimension()
						|| this.A.getColumnDimension() != this.b.getRowDimension()
						|| this.b.getColumnDimension() != 1
				)
			throw new ParamsException("Invalid parameters' dimensions\n");
	}

	public final double[] solve() {
		double[] ans = null;
		for (double[] x : this) {
			ans = x;
		}
		assert ans != null;

		return ans;
	}

	/**
	 * @return name of solution approach
	 */
	public String getName(){
		return this.getClass().getSimpleName();
	}


	private boolean hasLaunched = false;

	@Override
	public final Iterator<double[]> iterator() {
		if (!hasLaunched) {
			hasLaunched = true;
			return new ISolverIterator();
		} else {
			throw new IllegalStateException("ISolver iterator can not be requested again");
		}
	}


	private class ISolverIterator implements Iterator<double[]> {
		private Matrix prevX;
		private Matrix curX;

		public ISolverIterator() {
			// prevX = {+Inf, +Inf, ..., +Inf}, curX = {-Inf, -Inf, ..., -Inf}
			// ||curX - prevX|| = +Inf initially  =>  isPreciousEnough feels good
			double[] q = new double[A.getRowDimension()];
			Arrays.fill(q, Double.POSITIVE_INFINITY);
			prevX = new Matrix(q, q.length);
			curX = new Matrix(new double[q.length], q.length).minus(prevX);
		}

		@Override
		public boolean hasNext() {
			return !ISolver.this.isPreciousEnough(curX.minus(prevX));
		}

		@Override
		public double[] next() {
			prevX = curX.copy();
			curX = countNext();

			// check dimensions of given matrix are correct
			if (curX.getColumnDimension() != 1 || curX.getRowDimension() != A.getColumnDimension())
				throw new SolverException("Illegal return value of method \"countNext\" \n\tdimensions expected: " + A.getColumnDimension() + " x 1 \n\tgained: " + curX.getRowDimension() + " x " + curX.getColumnDimension() + "\n");

			return curX.getColumnPackedCopy();
		}
	}

	/**
	 * Calculates next version of x
	 */
	protected abstract Matrix countNext();

	/**
	 * Determines whether further calculation may be stopped
	 *
	 * @param deltaX - difference between two last versions of <t>x</t>
	 */
	protected abstract boolean isPreciousEnough(Matrix deltaX);


	public class SolverException extends RuntimeException {
		public SolverException(String message) {
			super(message);
		}
	}


	public class ParamsException extends RuntimeException {
		public ParamsException(String message) {
			super(message);
		}
	}

}
