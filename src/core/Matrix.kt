package core

class Matrix {
    val M: Int             // number of rows
    val N: Int             // number of columns
    val data: Array<DoubleArray>   // M-by-N array

    // create M-by-N matrix of 0's
    constructor(M: Int, N: Int) {
        this.M = M
        this.N = N
        data = Array(M) { DoubleArray(N) }
    }

    // create matrix based on 2d array
    constructor(data: Array<DoubleArray>) {
        M = data.size
        N = data[0].size
        this.data = Array(M) { DoubleArray(N) }
        for (i in 0 until M)
            for (j in 0 until N)
                this.data[i][j] = data[i][j]
    }

    // copy constructor
    private constructor(A: Matrix) : this(A.data) {}

    // swap rows i and j
    private fun swap(i: Int, j: Int) {
        val temp = data[i]
        data[i] = data[j]
        data[j] = temp
    }

    // create and return the transpose of the invoking matrix
    fun transpose(): Matrix {
        val A = Matrix(N, M)
        for (i in 0 until M)
            for (j in 0 until N)
                A.data[j][i] = this.data[i][j]
        return A
    }

    // return C = A + B
    operator fun plus(B: Matrix): Matrix {
        val A = this
        if (B.M != A.M || B.N != A.N) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(M, N)
        for (i in 0 until M)
            for (j in 0 until N)
                C.data[i][j] = A.data[i][j] + B.data[i][j]
        return C
    }


    // return C = A - B
    operator fun minus(B: Matrix): Matrix {
        val A = this
        if (B.M != A.M || B.N != A.N) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(M, N)
        for (i in 0 until M)
            for (j in 0 until N)
                C.data[i][j] = A.data[i][j] - B.data[i][j]
        return C
    }

    // does A = B exactly?
    fun eq(B: Matrix): Boolean {
        val A = this
        if (B.M != A.M || B.N != A.N) throw RuntimeException("Illegal matrix dimensions.")
        for (i in 0 until M)
            for (j in 0 until N)
                if (A.data[i][j] != B.data[i][j]) return false
        return true
    }

    // return C = A * B
    operator fun times(B: Matrix): Matrix {
        val A = this
        if (A.N != B.M) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(A.M, B.N)
        for (i in 0 until C.M)
            for (j in 0 until C.N)
                for (k in 0 until A.N)
                    C.data[i][j] += A.data[i][k] * B.data[k][j]
        return C
    }


    // return x = A^-1 b, assuming A is square and has full rank
    fun solve(rhs: Matrix): Matrix {
        if (M != N || rhs.M != N || rhs.N != 1)
            throw RuntimeException("Illegal matrix dimensions.")

        // create copies of the data
        val A = Matrix(this)
        val b = Matrix(rhs)

        // Gaussian elimination with partial pivoting
        for (i in 0 until N) {

            // find pivot row and swap
            var max = i
            for (j in i + 1 until N)
                if (Math.abs(A.data[j][i]) > Math.abs(A.data[max][i]))
                    max = j
            A.swap(i, max)
            b.swap(i, max)

            // singular
            if (A.data[i][i] == 0.0) throw RuntimeException("Matrix is singular.")

            // pivot within b
            for (j in i + 1 until N)
                b.data[j][0] -= b.data[i][0] * A.data[j][i] / A.data[i][i]

            // pivot within A
            for (j in i + 1 until N) {
                val m = A.data[j][i] / A.data[i][i]
                for (k in i + 1 until N) {
                    A.data[j][k] -= A.data[i][k] * m
                }
                A.data[j][i] = 0.0
            }
        }

        // back substitution
        val x = Matrix(N, 1)
        for (j in N - 1 downTo 0) {
            var t = 0.0
            for (k in j + 1 until N) {
                t += A.data[j][k] * x.data[k][0]
            }
            x.data[j][0] = (b.data[j][0] - t) / A.data[j][j]
        }
        return x

    }

    companion object {

        // create and return a random M-by-N matrix with values between 0 and 1
        fun random(M: Int, N: Int): Matrix {
            val A = Matrix(M, N)
            for (i in 0 until M)
                for (j in 0 until N)
                    A.data[i][j] = Math.random()
            return A
        }

        // create and return the N-by-N identity matrix
        fun identity(N: Int): Matrix {
            val I = Matrix(N, N)
            for (i in 0 until N)
                I.data[i][i] = 1.0
            return I
        }

    }

    operator fun set(i: Int, j: Int, value: Double) {
        data[i][j] = value
    }

    operator fun get(i: Int, j: Int): Double = data[i][j]

}