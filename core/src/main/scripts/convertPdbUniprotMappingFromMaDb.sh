#! /usr/bin/env python
# script to generate PDB-Uniprot residue mapping file from MutationAssessor database.

import re
import os
import sys
import getopt
import MySQLdb

# example: "-99 -5--1 0 1-3 7-200 204 208 10001"
range_re = re.compile('(-?[0-9]+)(-(-?[0-9]+))?')
def range_to_list(range_str):
    idx = []
    ranges = range_str.split(' ')
    for range in ranges:
        re_result = range_re.match(range)
        start = int(re_result.group(1))
        end = int(re_result.group(3)) if re_result.group(3)!=None else start
        if end<start:
            print >> ERROR_FILE, 'end smaller than start'
            sys.exit(2)
        idx.extend(range(start, end+1))
    return idx

def export_row(row):
    print("%s\t%s\t%i\t%s\t%i" % (row[0], row[1], range_to_list(row[5])[row[2]], row[3], row[4]))
    
def main():

    # parse command line options
    try:
        opts, args = getopt.getopt(sys.argv[1:], '', ['host=', 'user=', 'psw='])
    except getopt.error, msg:
        print >> ERROR_FILE, msg
        usage()
        sys.exit(2)

    # process the options
    host = opts['--host']
    user = opts['user']
    psw = opts['psw']

    db = MySQLdb.connect(host,user,psw)
    cursor = db.cursor()
    cursor.execute("select distinct pp.pdbid, pp.chcode, ppr.rpdb, mb.seqID, ppr.rprot+mb.mbegin-1, pmr.res "+
                   "from pdb_protr ppr, pdb_prot pp, msa_built mb, pdb_mol pm, pdb_molr pmr "+
                   "where ppr.ppid=pp.id and pp.msaid=mb.id and pp.pdbid=pm.pdbid and pp.chcode=pm.chain and "+
                   "pp.pdbid=pmr.pdbid and pm.molid=pmr.molid and pm.type='protein' and mb.seqID like '%_HUMAN' "+
                   "and pp.identp=100;")
    print("#pdb_id\tchain\tpdb_res\tuniprot_id\tuniprot_res")
    for row in cursor:
        export_row(row)
    db.close()

if __name__ == '__main__':
    main()