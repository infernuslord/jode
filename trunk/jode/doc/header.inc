<?php
$version="1.0.93";

function selflink($link) {
  echo "<a href=\"./";
  if ($link != "jode") {
    if (ereg("#", $link)) {
      $link = ereg_replace("#", ".php#", $link);
    } else {
      $link .= ".php";
    }
    echo "$link";
  }
  echo "\">";
}

function sflink($link) {
  echo "<a href=\"http://sourceforge.net/$link?group_id=3790\">";
}
?><!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 3.0//EN">
<html>
<head>
<title>Java Optimize and Decompile Environment (JODE)</title>
<meta name="date" content="2000-06-30">
<meta name="description" content="JODE - Java Optimize and Decompile Environment.">
<meta name="author" content="Jochen Hoenicke">
<meta name="keywords" content="jode, java, decompiler, obfuscator, deobfuscator, reverse engineering, free, GPL">
</head>

<BODY bgcolor=#FFFFFF topmargin="0" bottommargin="0" leftmargin="0" rightmargin="0" marginheight="0" marginwidth="0">
<table cellpadding=4 cellspacing=1 width=100%>
<tr><td>
<?php /* <CENTER><img src="constructionint.gif" alt="Under Construction, "><br>
beware broken links</CENTER> */ ?>
</td>
<td> <img src="jode-logo.gif" alt="JODE" width=286 height=110></td>
</tr>
<tr>
<td valign="top">
<?php require("menu.inc"); ?>
</td>
<td valign="top">
