<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
    <xsl:template match="business">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4" page-height="29.7cm" page-width="21cm" margin-top="3cm"
                                       margin-bottom="3cm" margin-left="3cm" margin-right="3cm">

                    <fo:region-body margin-top="2mm" margin-bottom="2mm"/>
                    <fo:region-before extent="2mm"   background-color="lightgrey"/>
                    <fo:region-after extent="2mm"  background-color="lightgrey"/>
                    <fo:region-start extent="2mm" background-color="lightgrey"/>
                    <fo:region-end extent="2mm" background-color="lightgrey"/>

                </fo:simple-page-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="A4">
                <fo:flow flow-name="xsl-region-body">
                    <fo:block border-bottom-width="0.05pt"
                              border-bottom-style="solid"
                              border-bottom-color="lightgrey"
                              font-weight="500"
                              font-size="26pt"
                              text-align="center"
                              space-after="10mm">
                        <fo:block padding="3mm">
                            <xsl:value-of select="businessName"/>
                        </fo:block>
                    </fo:block>
                    <fo:block text-align="center">
                        <!-- FO scales uniform image, if want non-uniform then use scaling="non-uniform" -->
                        <fo:external-graphic
                                content-height="scale-to-fit"
                                height="2.50in"
                                content-width="2.50in">
                            <xsl:attribute name="src">
                                <xsl:value-of select="imageLocationCodeQR" />
                            </xsl:attribute>
                        </fo:external-graphic>
                    </fo:block>
                    <fo:block text-align="center"
                              space-before="8mm">
                        <!-- FO scales uniform image, if want non-uniform then use scaling="non-uniform" -->
                        <fo:external-graphic
                                content-width="scale-to-fit"
                                content-height="110%"
                                width="110%"
                                scaling="uniform"
                                src="../../../static2/internal/img/logo.png"/>
                    </fo:block>
                    <fo:block font-size="smaller"
                              text-align="center"
                              space-after="20mm">
                        <fo:block>
                            <fo:inline color="grey">&#169; &#174; 2019 NoQueue</fo:inline>
                        </fo:block>
                    </fo:block>
                    <fo:block border-bottom-width="0.05pt"
                              border-bottom-style="solid"
                              border-bottom-color="lightgrey"
                              font-weight="500"
                              font-size="18pt"
                              text-align="center"
                              space-after="10mm">
                        <fo:block padding="3mm">
                            Download NoQApp
                        </fo:block>
                    </fo:block>
                    <fo:block font-size="10pt">
                        <fo:table table-layout="fixed" width="100%" border-collapse="separate">
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell>
                                        <fo:block text-align="center">
                                            <fo:external-graphic
                                                    content-width="scale-to-fit"
                                                    content-height="80%"
                                                    width="80%"
                                                    scaling="uniform"
                                                    src="../../../static2/internal/img/apple-store.png"/>
                                        </fo:block>
                                    </fo:table-cell>

                                    <fo:table-cell>
                                        <fo:block text-align="center">
                                            <fo:external-graphic
                                                    content-width="scale-to-fit"
                                                    content-height="80%"
                                                    width="80%"
                                                    scaling="uniform"
                                                    src="../../../static2/internal/img/google-play.png"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell>
                                        <fo:block text-align="center">
                                            <fo:external-graphic
                                                    content-width="scale-to-fit"
                                                    content-height="50%"
                                                    width="50%"
                                                    scaling="uniform"
                                                    padding-top="10px"
                                                    src="../../../static2/internal/img/NoQApp-apple.png"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell>
                                        <fo:block text-align="center">
                                            <fo:external-graphic
                                                    content-width="scale-to-fit"
                                                    content-height="50%"
                                                    width="50%"
                                                    scaling="uniform"
                                                    padding-top="10px"
                                                    src="../../../static2/internal/img/NoQApp-google.png"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
</xsl:stylesheet>