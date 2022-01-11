<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:outline="http://wkhtmltopdf.org/outline"
                xmlns="http://www.w3.org/1999/xhtml">
  <xsl:output doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
              doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
              indent="yes" />
  <xsl:template match="outline:outline">
    <html>
      <head>
        <title>Table of Contents</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <style>
          body{
          font-family: "Open Sans Light",Helvetica,arial,sans-serif;
          color:#4d4e53;
          }
          h1 {
          text-align: center;
          font-size: 14px;
          }
          div {
          margin-bottom:4px;
          border-bottom: dotted 1px #CCCCCC;
          padding-bottom: 4px;
          font-size: 14px;
          margin-left: auto;
          margin-right: auto;
          }
          span {float: right;}
          li {list-style: none;}
          ul {
          font-size: 20px;
          font-family: arial;
          }
          ul ul {font-size: 80%; }
          ul ul ul { display:none; }
          ul {padding-left: 0em;}
          ul ul {padding-left: 0em;}
          a {text-decoration:none; color: black;}
          ul ul li ul.level-2 { display: none; }
          ul ul li.level-2 { display: none; }
          ul ul li.level-3 { display: none; }
          ul ul li.level-4 { display: none; }
        </style>
      </head>
      <body>
        <h1>Table of Contents</h1>
        <ul><xsl:apply-templates select="outline:item"/></ul>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="outline:item">
    <xsl:variable name="depth" select="count(ancestor::*)"/>
    <li>
      <xsl:if test="((@title!='') and ($depth = 2))">
        <div>
          <a>
            <xsl:if test="@link">
              <xsl:attribute name="href"><xsl:value-of select="@link"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="@backLink">
              <xsl:attribute name="name"><xsl:value-of select="@backLink"/></xsl:attribute>
            </xsl:if>
            <xsl:value-of select="@title" />
          </a>
          <span> <xsl:value-of select="@page" /> </span>
        </div>
      </xsl:if>
      <ul>
        <xsl:comment>added to prevent self-closing tags in QtXmlPatterns</xsl:comment>
        <xsl:apply-templates select="outline:item"/>
      </ul>
    </li>
  </xsl:template>
</xsl:stylesheet>