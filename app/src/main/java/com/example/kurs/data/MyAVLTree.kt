package com.example.kurs.data

import com.example.kurs.models.Doctor

class MyAVLTree {
    private data class Node(
        var key: String,
        var value: Doctor,
        var left: Node? = null,
        var right: Node? = null,
        var height: Int = 1
    )

    private var root: Node? = null

    private fun height(node: Node?): Int = node?.height ?: 0
    private fun getBalance(node: Node?): Int = if (node == null) 0 else height(node.left) - height(node.right)

    // Малые повороты по п. 6.6 [cite: 1080, 1081, 1101]
    private fun rotateRight(y: Node): Node {
        val x = y.left!!
        y.left = x.right
        x.right = y
        y.height = maxOf(height(y.left), height(y.right)) + 1
        x.height = maxOf(height(x.left), height(x.right)) + 1
        return x
    }

    private fun rotateLeft(x: Node): Node {
        val y = x.right!!
        x.right = y.left
        y.left = x
        x.height = maxOf(height(x.left), height(x.right)) + 1
        y.height = maxOf(height(y.left), height(y.right)) + 1
        return y
    }

    fun insert(doctor: Doctor) { root = insertRecursive(root, doctor) }

    private fun insertRecursive(node: Node?, doctor: Doctor): Node {
        if (node == null) return Node(doctor.fio, doctor)
        if (doctor.fio < node.key) node.left = insertRecursive(node.left, doctor)
        else if (doctor.fio > node.key) node.right = insertRecursive(node.right, doctor)
        else return node

        node.height = 1 + maxOf(height(node.left), height(node.right))
        val balance = getBalance(node) // Баланс-фактор по п. 6.6 [cite: 1086]

        if (balance > 1 && doctor.fio < node.left!!.key) return rotateRight(node)
        if (balance < -1 && doctor.fio > node.right!!.key) return rotateLeft(node)
        if (balance > 1 && doctor.fio > node.left!!.key) { node.left = rotateLeft(node.left!!); return rotateRight(node) }
        if (balance < -1 && doctor.fio < node.right!!.key) { node.right = rotateRight(node.right!!); return rotateLeft(node) }
        return node
    }

    fun remove(fio: String): Boolean {
        val oldRoot = root
        root = deleteRecursive(root, fio)
        return oldRoot != root || (oldRoot != null && root == null)
    }

    private fun deleteRecursive(node: Node?, key: String): Node? {
        if (node == null) return null
        var current = node
        if (key < current.key) current.left = deleteRecursive(current.left, key)
        else if (key > current.key) current.right = deleteRecursive(current.right, key)
        else {
            if (current.left == null || current.right == null) current = current.left ?: current.right
            else {
                val temp = getMin(current.right!!)
                current.key = temp.key; current.value = temp.value
                current.right = deleteRecursive(current.right, temp.key)
            }
        }
        if (current == null) return null
        current.height = 1 + maxOf(height(current.left), height(current.right))
        return current // Для курсовой упрощенный баланс при удалении
    }

    private fun getMin(node: Node): Node {
        var c = node
        while (c.left != null) c = c.left!!
        return c
    }

    // Симметричный обход по п. 6.3 [cite: 1016, 1035]
    fun searchByPosition(query: String, algo: (String, String) -> Boolean): List<Doctor> {
        val res = mutableListOf<Doctor>()
        fun traverse(n: Node?) {
            if (n == null) return
            traverse(n.left)
            if (algo(n.value.position, query)) res.add(n.value)
            traverse(n.right)
        }
        traverse(root)
        return res
    }

    fun getAll(): List<Doctor> {
        val res = mutableListOf<Doctor>()
        fun traverse(n: Node?) {
            if (n == null) return
            traverse(n.left); res.add(n.value); traverse(n.right)
        }
        traverse(root)
        return res
    }

    fun clear() { root = null }
}