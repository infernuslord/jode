<?php
$menu = array(
  "Jode",
     array("<B>Home</B>"  , "selflink", "jode",
           "Project page" , "sflink", "project/",
           "-", "-", "-",
           "Applet"       , "selflink", "applet",
           "Download"     , "selflink", "download",
	   "Documentation", "selflink", "usage",
           "License"      , "selflink", "license",
           "History"      , "selflink", "history",
           "Links"        , "selflink", "links",
           "Blue Sky"     , "selflink", "bluesky"),
  "Feedback",
     array("Bug Tracking" , "sflink", "bugs/",
	   "Public Forums" , "sflink", "forum/",
	   "Mailing List" , "link",
               "http://lists.sourceforge.net/mailman/listinfo/jode-users",
	   "Private Mail" , "link",
               "http://sourceforge.net/sendmessage.php?touser=18252"),
  "Download",
     array("FTP server", "link", "ftp://jode.sourceforge.net/pub/jode/",
           "Source releases", "sflink", "project/filelist.php"));

$images = array(
  "Powered by ", "http://sourceforge.net/sflogo.php?group_id=3790&type=1",
	"SourceForge", "http://sourceforge.net",
  "Best viewed with ", "a-logo.gif", 
	"Any Browser", "http://www.anybrowser.org/campaign/");

if (eregi("^Lynx", $HTTP_USER_AGENT)) {

  reset($menu);
  while (list($dummy, $header) = each($menu)) {
    list($dummy, $subitems) = each($menu);

    echo "<b>$header:</b>\n";
    reset($subitems);
    while (list($dummy, $name) = each($subitems)) {
      list($dummy, $type) = each($subitems);
      list($dummy, $link) = each($subitems);
      if ($type == "selflink") {
        selflink($link);
      } else if ($type == "sflink") {
	sflink($link);
      } else if ($type == "-") {
        echo "<br>\n";
	continue;
      } else if ($type == "link") {
	echo "<a href=\"$link\">";
      }
      $name = ereg_replace(" ", "&nbsp;", $name);
      echo "$name</a>";
      if (current($subitems)) {
	echo " |\n";
      }
    }
    echo "<br>\n\n";
  }
  echo "<right>\n";
  reset($images);
  while (list($dummy, $label) = each($images)) {
    list($dummy, $src) = each($images);
    list($dummy, $alt) = each($images);
    list($dummy, $link) = each($images);
    echo "$label<a href=\"$link\"><img src=\"$src\" border=0";
    echo " width=88 height=31 alt=\"$alt\"></a>\n";
  }
  echo "</right>\n";
} else {
  echo "<table cellspacing=0 cellpadding=3 width=100%";
  echo " border=0 bgcolor=eeeef8>\n";

  reset($menu);
  while (list($dummy, $header) = each($menu)) {
    list($dummy, $subitems) = each($menu);
    echo "<tr bgcolor=\"7272cc\">\n<td align=\"center\">";
    echo "<font color=\"ffffff\"><b>$header</b></font></td></tr>\n";
    echo "<tr><td align=\"right\">\n";
    reset($subitems);
    while (list($dummy, $name) = each($subitems)) {
      list($dummy, $type) = each($subitems);
      list($dummy, $link) = each($subitems);
      if ($type == "selflink") {
        selflink($link);
      } else if ($type == "sflink") {
	sflink($link);
      } else if ($type == "-") {
        echo "<br>\n";
	continue;
      } else if ($type == "link") {
	echo "<a href=\"$link\">";
      }
      $name = ereg_replace(" ", "&nbsp;", $name);
      echo "$name</a>";
      if (current($subitems)) {
	echo "<br>\n";
      }
    }
  }
  echo "</td></tr>\n";
  echo "<tr bgcolor=\"ffffff\"><td align=\"center\">";

  reset($images);
  while (list($dummy, $label) = each($images)) {
    list($dummy, $src) = each($images);
    list($dummy, $alt) = each($images);
    list($dummy, $link) = each($images);
    echo "<br>$label<br>\n";
    echo "<a href=\"$link\"><img src=\"$src\" border=0";
    echo " width=88 height=31 alt=\"$alt\"></a><br>\n";
  }
  echo "</td></tr></table>\n";
}
?>
