package org.example.config

import org.example.AppApplication
import org.example.annotation.AppComponent
import org.example.annotation.BeanInject
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

object Starter {
    private val CONTAINER: MutableMap<String, Any?> = Hashtable()
    fun run(clazz: Class<AppApplication>, args: Array<String>) {
        val path = Objects.requireNonNull(clazz.getResource("")).path
        val file = File(path)
        val pkg = clazz.getPackage().name + "."
        try {
            init(file, pkg)
            operate()
            println(System.`in`.read())
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(Exception::class)
    private fun init(file: File, pkg: String) {
        for (listFile in Objects.requireNonNull(file.listFiles())) {
            if (listFile.isDirectory) {
                val pkgName = pkg + listFile.name + "."
                init(listFile, pkgName)
            } else {
                val s = pkg + listFile.name.split(".class".toRegex()).toTypedArray()[0]
                reflect(Class.forName(s))
            }
        }
    }

    @Throws(Exception::class)
    private fun reflect(clazz: Class<*>) {
        val name = clazz.simpleName
        val s = name[0].toString().lowercase(Locale.getDefault()) + name.substring(1)
        if (CONTAINER[s] != null) return
        //        判断是普通类
        if (clazz.modifiers == Modifier.PUBLIC) {
//            遍历类的所有的注解
            for (anno in clazz.declaredAnnotations) {
//            判断是否声明了创建类的注解
                if (anno is AppComponent) {
                    CONTAINER[s] = clazz.newInstance()
                    break
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun operate() {
        for (value in CONTAINER.values) {
            val clazz: Class<*> = value!!.javaClass
            for (f in clazz.declaredFields) {
                for (anno in f.declaredAnnotations) {
                    //                        声明了自动注入的注解
                    if (anno is BeanInject) {
                        inject(value, f)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun inject(o: Any?, f: Field) {
        val type = f.type
        f.isAccessible = true
        if (type.isInterface) {
            for (value in CONTAINER.values) {
//                            查找是否已存在符合它的子类
                if (type.isAssignableFrom(value!!.javaClass)) {
                    f[o] = value
                    break
                }
            }
        } else {
            val name = type.simpleName
            val s = name[0].toString().lowercase(Locale.getDefault()) + name.substring(1)
            val obj = CONTAINER[s]
            if (obj != null) {
                f[o] = obj
            } else {
                reflect(type)
                f[o] = CONTAINER[s]
            }
        }
    }
}