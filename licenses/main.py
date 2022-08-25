#!/usr/bin/env python
import subprocess
import xml.etree.cElementTree as ET
from http.server import BaseHTTPRequestHandler, HTTPServer
import re
import os
import time
import datetime
import os.path

import io
import xlsxwriter  # external library

class CustomHandler(BaseHTTPRequestHandler):

    def getXml(self):
        with open('datalicweb_ts.txt', 'r') as f:
            content = f.read()
        lastupdate = 'Last update: ' + time.ctime(os.path.getmtime('datalicweb_ts.txt'))
        content_new = '<?xml version="1.0"?>\n\t<alldata>\n' + content + '\t</alldata>\n'
        content_new = re.sub('(\tDassault.Systemes.\W.*\n(?:\t{2,8}.*\n)*)', r'\t<ds3delic>\n\1\t</ds3delic>\n',
                             content_new, flags=re.M)
        content_new = re.sub('(\tDassault.Systemes.V5.*\n(?:\t{2,8}.*\n)*)', r'\t<dsv5lic>\n\1\t</dsv5lic>\n',
                             content_new, flags=re.M)
        content_new = re.sub('\t(Dassault.*\))', r'\t\t<dslicinfo>\1</dslicinfo>', content_new, flags=re.M)
        content_new = re.sub('(\t\t(?:[A-Z0-9]).*\n(?:\t\t\t\t*([A-Z0-9]?).*\n)*)',
                             r'\t\t<licname>\n\1\t\t</licname>\n', content_new, flags=re.M)
        content_new = re.sub('(\t\t\t(?:[A-Z0-9]?).*\n(?:\t\t\t\t([A-Z0-9]?).*\n)*)',
                             r'\t\t\t<licuser>\n\1\n\t\t\t</licuser>\n', content_new, flags=re.M)
        content_new = re.sub('\t\t\t\t(([A-Z0-9]?).*)\n', r'\t\t\t\t<licprocess>\1</licprocess>\n', content_new,
                             flags=re.M)
        content_new = re.sub('\t\t([A-Z0-9][A-Z0-9][A-Z0-9])', r'\t\tLicenseName: \1', content_new, flags=re.M)
        content_new = re.sub('LicenseName:\W*([A-Z0-9-]{3,30})\W*maxReleaseNumber',
                             r'\t<licensename>\1</licensename>\nmaxReleaseNumber', content_new, flags=re.M)
        content_new = re.sub('maxReleaseNumber:\W*([A-Z0-9-\.]{0,30})\W*maxReleaseDate',
                             r'\t\t\t<maxreleasenumber>\1</maxreleasenumber>\nmaxReleaseDate', content_new, flags=re.M)
        content_new = re.sub('maxReleaseDate:\W*([0-9].*[0-9])\W*expirationDate',
                             r'\t\t\t<maxreleasedate>\1</maxreleasedate>\nexpirationDate', content_new, flags=re.M)
        content_new = re.sub('expirationDate:\W*([0-9].*[0-9])\W*type',
                             r'\t\t\t<expirationDate>\1</expirationDate>\ntype', content_new, flags=re.M)
        content_new = re.sub('type:\W*(\w*)\W*count', r'\t\t\t<type>\1</type>\ncount', content_new, flags=re.M)
        content_new = re.sub('count:\W*([0-9]*)\W*inuse', r'\t\t\t<count>\1</count>\ninuse', content_new, flags=re.M)
        content_new = re.sub('inuse:\W*([0-9]*)\W*customerId', r'\t\t\t<inuse>\1</inuse>\ncustomerId', content_new,
                             flags=re.M)
        content_new = re.sub('customerId:\W*([0-9]*)\W*pricing\sstructure',
                             r'\t\t\t<customerid>\1</customerid>\npricingstructure', content_new, flags=re.M)
        content_new = re.sub('pricingstructure:\W*(.*)\W*$', r'\t\t\t<pricingstructure>\1</pricingstructure>',
                             content_new, flags=re.M)
        content_new = re.sub('internal.Id:\W*(\w.*\w)\W*granted.since', r'\t<internalid>\1</internalid>\ngrantedsince',
                             content_new, flags=re.M)
        content_new = re.sub('grantedsince:\W*([0-9].*[0-9])\W*last.used.at',
                             r'\t\t\t\t<grantedsince>\1</grantedsince>\nlastusedat', content_new, flags=re.M)
        content_new = re.sub('lastusedat:\W*([0-9].*[0-9])\W*by.user', r'\t\t\t\t<lastusedat>\1</lastusedat>\nbyuser',
                             content_new, flags=re.M)
        content_new = re.sub('byuser:\W*(\w.*\w)\W*on.host', r'\t\t\t\t<byuser>\1</byuser>\nonhost', content_new,
                             flags=re.M)
        content_new = re.sub('onhost:\W*(.*\.[0-9]*)\W.*$', r'\t\t\t\t<onhost>\1</onhost>', content_new, flags=re.M)
        return  content_new

    def do_GET(self):
        if self.headers['Authorization'] == None:
            self.send_response(401)
            self.send_header('WWW-Authenticate', 'Basic realm=\"Test\"')
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(bytes('no auth header received', 'UTF-8'))
            pass
        elif self.headers['Authorization'] == 'Basic YWE6YmI=':
            self.send_response(200)
            content_new = self.getXml()

            if self.path == '/excel':
                self.send_header('Content-Disposition', 'attachment; filename=licences.xlsx')
                self.send_header('Content-type',  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
                self.end_headers()

                output = io.BytesIO()
                workbook = xlsxwriter.Workbook(output, {'in_memory': True})
                worksheet = workbook.add_worksheet()

                root = ET.fromstring(content_new)

                licenses = {}

                for child in root.iter('licname'):
                    if child.find('type').text != "Token":
                        licenceName = child.find('licensename').text

                        users = []
                        for uuser in child.iter('licuser'):
                            users.append(uuser.find('byuser').text)

                        licence = licenses.get(licenceName)

                        if licence == None:
                            licenses.update({licenceName: {
                                "license": licenceName,
                                "inuse": int(child.find('inuse').text),
                                "count": int(child.find('count').text),
                                "users": users,
                            }})
                        else:
                            licence.update({
                                "license": licenceName,
                                "inuse": licence.get("inuse") + int(child.find('inuse').text),
                                "count": licence.get("count") + int(child.find('count').text),
                                "users": licence.get("users") + users,
                            })
                    #else: ??

                worksheet.write(0, 0, "Licence")
                worksheet.write(0, 1, "InUse")
                worksheet.write(0, 2, "Count")
                worksheet.write(0, 3, "Users")

                line_index = 0
                for licence in licenses.values():
                    line_index += 1
                    worksheet.write(line_index, 0, licence.get("license"))
                    worksheet.write(line_index, 1, licence.get("inuse"))
                    worksheet.write(line_index, 2, licence.get("count"))
                    worksheet.write(line_index, 3, ",".join(licence.get("users")))

                workbook.close()

                output.seek(0)

                self.wfile.write(output.read())
            elif self.path == "/":
                self.send_response(401)
                self.send_header('Content-type', 'text/html')
                self.end_headers()
                lastupdate = 'Last update: ' + time.ctime(os.path.getmtime('datalicweb_ts.txt'))
                startpage = "<!DOCTYPE html>\n<html>\n<head>\n<style>#myInput {width: 100%;  font-size: 16px;  padding: 12px 20px 12px 40px;  border: 1px solid #ddd;  margin-bottom: 12px;} abbr[title] {border-bottom: none !important; cursor: inherit !important; text-decoration: none !important;} table {font-size: 60%; font-family: arial, sans-serif; border-collapse: collapse; width: 100%;} td, th { border: 1px solid #dddddd; text-align: left; padding: 8px;} tr:nth-child(even) {background-color: #dddddd;}</style>\n</head>\n<body>\n<h2>IGA License Control Center</h2>\n<p><font size=\"3\">" + lastupdate + "</font>   <a href='/excel'><button>Download</button></a></p>\n<input type=\"text\" id=\"myInput\" onkeyup=\"myFunction()\" placeholder=\"Search criteria...\" title=\"Type in a name\">\n<table id=\"myTable\">\n<tr>\n<th>Trigram</th>\n<th>End</th>\n<th>Type</th>\n<th>InUse</th>\n<th>Count</th>\n<th>Users</th>\n</tr>"
                self.wfile.write(bytes(startpage, 'utf-8'))

                # text_file = open("Output.txt", "w")
                # text_file.write(content_new)
                # text_file.close()

                root = ET.fromstring(content_new)
                deltaday = datetime.timedelta(int(14))
                greenuserday = datetime.timedelta(days=int(3))
                yeluserday = datetime.timedelta(int(8))
                for child in root.iter('licname'):
                    if child.find('type').text != "Token":
                        useusers = ""
                        datalic1 = child.find('expirationDate').text.split(', ')
                        datalic2 = datalic1[0].split(".")
                        timelic1 = datalic1[1].split(":")
                        enddate = datetime.datetime(int(datalic2[2]), int(datalic2[1]), int(datalic2[0]), int(timelic1[0]),
                                                    int(timelic1[1]), int(timelic1[2])) - datetime.datetime.now().replace(
                            microsecond=0)
                        # enddate = enddate1.replace(microsecond=0)
                        if deltaday > enddate:
                            deltacolor = "red"
                        else:
                            deltacolor = "black"
                        level1 = '<tr>\n<td>' + '<abbr title=\"' + " expirationDate: " + child.find(
                            'expirationDate').text + '\">' + child.find(
                            'licensename').text + '</abbr>' + '</td>\n<td><font color=' + deltacolor + '>' + str(
                            enddate) + '</font></td>\n<td>' + child.find('type').text + '</td>\n<td>' + child.find(
                            'inuse').text + '</td>\n<td>' + child.find('count').text + '</td>\n'
                        # self.wfile.write(bytes(level1,'utf-8'))
                        for uuser in child.iter('licuser'):
                            timeuser1 = uuser.find('lastusedat').text.split(', ')
                            timeuser2 = timeuser1[0].split(".")
                            timeuser3 = timeuser1[1].split(":")
                            entertime1 = uuser.find('grantedsince').text.split(', ')
                            entertime2 = entertime1[0].split(".")
                            entertime3 = entertime1[1].split(":")
                            entertimeuser = datetime.datetime.now().replace(microsecond=0) - datetime.datetime(
                                int(entertime2[2]), int(entertime2[1]), int(entertime2[0]), int(entertime3[0]),
                                int(entertime3[1]), int(entertime3[2]))
                            daytimeuser = datetime.datetime.now().replace(microsecond=0) - datetime.datetime(
                                int(timeuser2[2]), int(timeuser2[1]), int(timeuser2[0]), int(timeuser3[0]),
                                int(timeuser3[1]), int(timeuser3[2]))
                            if daytimeuser < greenuserday:
                                usercolor = "darkgreen"
                            elif greenuserday <= daytimeuser <= yeluserday:
                                usercolor = "darkorange"
                            else:
                                usercolor = "firebrick"
                            useusers = useusers + '| ' + '<abbr title=\"' + " Internal id: " + uuser.find(
                                'internalid').text + "\n Granted: " + str(entertimeuser) + " ago \n Last enter: " + str(
                                daytimeuser) + " ago \n host: " + uuser.find(
                                'onhost').text + '\"><font color=' + usercolor + '>' + uuser.find(
                                'byuser').text + '</font></abbr>' + ' |'
                        data1 = level1 + '<td>' + useusers + '</td>\n</tr>\n'
                        self.wfile.write(bytes(data1, 'utf-8'))
                    else:
                        useusers2 = ""
                        level12 = '<tr>\n<td>' + '<abbr title=\"' + " expirationDate: " + child.find(
                            'expirationDate').text + '\">' + child.find(
                            'licensename').text + '</abbr>' + '</td>\n<td><font color=' + deltacolor + '>' + str(
                            enddate) + '</font></td>\n<td>' + child.find('type').text + '</td>\n<td>' + child.find(
                            'inuse').text + '</td>\n<td>' + child.find('count').text + '</td>\n'
                        for uuser in child.iter('licuser'):
                            licprocess2 = uuser.find('licprocess').text.split('(')
                            tokens2 = uuser.find('licprocess').text.split('tokens:')
                            useusers2 = useusers2 + '<abbr title=\"' + " Info: " + uuser.find(
                                'licprocess').text + '\"><font color=black>' + licprocess2[0] + ' : ' + tokens2[
                                            1] + '</font></abbr>' + ' |'
                        data2 = level12 + '<td>' + useusers2 + '</td>\n</tr>\n'
                        self.wfile.write(bytes(data2, 'utf-8'))
                endpage = "</table>\n<script>function myFunction() {  var input, filter, table, tr, td, i;  input = document.getElementById(\"myInput\");  filter = input.value.toUpperCase();  table = document.getElementById(\"myTable\");  tr = table.getElementsByTagName(\"tr\");  for (i = 0; i < tr.length; i++) {    td = tr[i].getElementsByTagName(\"td\")[0];    td1 = tr[i].getElementsByTagName(\"td\")[1];    td2 = tr[i].getElementsByTagName(\"td\")[2];    td3 = tr[i].getElementsByTagName(\"td\")[5];    if (td||td1) {      if ((td.innerHTML.toUpperCase().indexOf(filter) > -1)||(td1.innerHTML.toUpperCase().indexOf(filter) > -1)||(td2.innerHTML.toUpperCase().indexOf(filter) > -1)||(td3.innerHTML.toUpperCase().indexOf(filter) > -1)) {        tr[i].style.display = \"\";      } else {        tr[i].style.display = \"none\";      }    }         }}</script>\n</body>\n</html>"
                self.wfile.write(bytes(endpage, 'utf-8'))


def main():
    httpd = HTTPServer(('10.10.33.2', 10001), CustomHandler)
    # httpd = HTTPServer(('127.0.0.1', 10001), CustomHandler)
    try:
        print('started httpd...')
        httpd.serve_forever()
    except KeyboardInterrupt:
        print('^C received, shutting down server')
        httpd.socket.close()

main()
