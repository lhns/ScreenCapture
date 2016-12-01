package org.lolhens.screencapture

import scala.util.Random

/**
  * Created by pierr on 01.12.2016.
  */
object Test {
  def main = {
    val char = "#"
    val treeTop = (1 until 20).filter(_ % 2 == 0).flatMap(i => List(0, i + Random.nextInt(i)))
    val tree = (treeTop :+ (treeTop.last * 0.6).toInt) ++ List(0, 0, 2, 2)
    val halfTreeString = tree.map(width => (char * width).padTo(tree.max, " ").mkString(""))
    val treeString = halfTreeString.map { line => s"${line.reverse.mkString("")}||$line" }
    val string = treeString.mkString("\n")
    println(string)
  }
}
