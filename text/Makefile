# settings precedence: command line>usercfg.mk>{windows,unix}.mk

# user settings
# include before variable definitions
ifneq ($(wildcard usercfg.mk),)
	include usercfg.mk
endif

# platform specific settings
# include before variable definitions
ifeq ($(OS),Windows_NT)
	include windows.mk
else
	include unix.mk
endif

# Ghostscript-based pdf postprocessing
include compress.mk

# Config file
MKRC ?= latexmkrc

# Source .tex file
SOURCE ?= dissertation

# LaTeX compiler output .pdf file
TARGET ?= $(SOURCE)

# LaTeX version:
# -pdf		= pdflatex
# -pdfdvi	= pdflatex with dvi
# -pdfps	= pdflatex with ps
# -pdfxe	= xelatex with dvi (faster than -xelatex)
# -xelatex	= xelatex without dvi
# -pdflua	= lualatex with dvi  (faster than -lualatex)
# -lualatex	= lualatex without dvi
BACKEND ?= -pdfxe

# Do not modify the section below. Edit usercfg.mk instead.
DRAFTON ?= # 1=on;0=off
SHOWMARKUP ?= # 1=on;0=off
FONTFAMILY ?= # 0=CMU;1=MS fonts;2=Liberation fonts
ALTFONT ?= # 0=Computer Modern;1=pscyr;2=XCharter
USEBIBER ?= # 0=bibtex8;1=biber
USEFOOTCITE ?= # 0=no;1=yes
BIBGROUPED ?= # 0=no;1=yes
IMGCOMPILE ?= # 1=on;0=off
NOTESON ?= # 0=off;1=on, separate slide;2=on, same slide
LATEXFLAGS ?= -halt-on-error -file-line-error
LATEXMKFLAGS ?= -silent
BIBERFLAGS ?= # --fixinits
REGEXDIRS ?= . Dissertation common # distclean dirs
TIMERON ?= # show CPU usage
TIKZFILE ?= # .tikz file for tikz rule
USEDEV ?= # use development version

# Makefile options
MAKEFLAGS := -s
.DEFAULT_GOAL := all
.NOTPARALLEL:

export DRAFTON
export SHOWMARKUP
export FONTFAMILY
export ALTFONT
export USEBIBER
export USEFOOTCITE
export BIBGROUPED
export IMGCOMPILE
export NOTESON
export LATEXFLAGS
export BIBERFLAGS
export REGEXDIRS
export TIMERON
export TIKZFILE
export USEDEV


define compile
	latexmk -norc -r $(MKRC) $(LATEXMKFLAGS) $(BACKEND) -jobname=$(TARGET) $(SOURCE)
endef

dissertation: TARGET=dissertation
dissertation: SOURCE=dissertation
dissertation:
	$(compile)
	"$(MAKE)" clean

distclean-target:
	latexmk -norc -r $(MKRC) -f $(LATEXMKFLAGS) $(BACKEND) -jobname=$(TARGET) -c $(SOURCE)

clean:
	"$(MAKE)" SOURCE=dissertation TARGET=dissertation distclean-target
	"$(MAKE)" SOURCE=common TARGET=common distclean-target


.PHONY:  dissertation clean distclean-target
