define("DS/SWTUtils/ui", ["DS/ENO6WPlugins/jQuery", "DS/SWTUtils/api",], function ($, api) {
        var ui = {
            autocomplete: function (elementId, arr) {
                var currentFocus;
                let inp = document.getElementById(elementId)
                inp.addEventListener("input", function (e) {
                    var a, b, i, val = this.value;
                    closeAllLists();
                    if (!val) {
                        return false;
                    }
                    currentFocus = -1;
                    a = document.createElement("div");
                    a.setAttribute("id", this.id + "autocomplete-list");
                    a.setAttribute("class", "autocomplete-items");
                    this.parentNode.appendChild(a);
                    for (i = 0; i < arr.length; i++) {
                        if (arr[i].substr(0, val.length).toUpperCase() == val.toUpperCase()) {
                            b = document.createElement("DIV");
                            b.innerHTML = "<strong>" + arr[i].substr(0, val.length) + "</strong>";
                            b.innerHTML += arr[i].substr(val.length);
                            b.innerHTML += "<input type='hidden' value='" + arr[i] + "'>";
                            b.addEventListener("click", function (e) {
                                inp.value = this.getElementsByTagName("input")[0].value;
                                inp.dispatchEvent(new Event('keyup'));
                                closeAllLists();
                            });
                            a.appendChild(b);
                        }
                    }
                });
                inp.addEventListener("keydown", function (e) {
                    var x = document.getElementById(this.id + "autocomplete-list");
                    if (x) x = x.getElementsByTagName("div");
                    if (e.keyCode == 40) {
                        currentFocus++;
                        addActive(x);
                    } else if (e.keyCode == 38) {
                        currentFocus--;
                        addActive(x);
                    } else if (e.keyCode == 13) {
                        e.preventDefault();
                        if (currentFocus > -1) {
                            if (x) x[currentFocus].click();
                        }
                    }
                });

                function addActive(x) {
                    if (!x) return false;
                    removeActive(x);
                    if (currentFocus >= x.length) currentFocus = 0;
                    if (currentFocus < 0) currentFocus = (x.length - 1);
                    x[currentFocus].classList.add("autocomplete-active");
                }

                function removeActive(x) {
                    for (var i = 0; i < x.length; i++) {
                        x[i].classList.remove("autocomplete-active");
                    }
                }

                function closeAllLists(elmnt) {
                    var x = document.getElementsByClassName("autocomplete-items");
                    for (var i = 0; i < x.length; i++) {
                        if (elmnt != x[i] && elmnt != inp) {
                            x[i].parentNode.removeChild(x[i]);
                        }
                    }
                }

                document.addEventListener("click", function (e) {
                    closeAllLists(e.target);
                });
            },
            sort_columns: {},
            table: function (table_id, column_names, data) {
                if (ui.sort_ctrl == null) {
                    document.body.onclick = function (e) {

                    }
                    ui.sort_ctrl = false
                }

                function sort(data) {
                    $("#" + table_id + " tbody tr:not(:first)").remove()

                    var table = $("#" + table_id)
                    data.forEach(function (item) {
                        let tr = $("<tr>").css('background-color', item.color);
                        column_names.forEach(function (columns_name) {
                            tr.append($("<td>").text(item[columns_name]))
                        })
                        if (item.id != null)
                            tr.on("click", function () {
                                window.open(api.url(item.id), '_blank');
                            })
                        table.append(tr);
                    })
                }

                $("#" + table_id + " tbody tr").children().click(function (e) {
                    var index = $(this).parent().children().index($(this))
                    var sort_columns = ui.sort_columns[table_id] || []
                    if (e.ctrlKey) {
                        sort_columns.push("+" + column_names[index])
                    } else {
                        var asc = sort_columns.length === 1 && sort_columns[0] === "+" + column_names[index]
                        sort_columns = [(asc ? "-" : "+") + column_names[index]]
                    }
                    ui.sort_columns[table_id] = sort_columns

                    for (let index in column_names) {
                        let column_name = column_names[index]
                        let col = $("#" + table_id + " tbody tr:first td").eq(index)
                        col.removeClass("col-ask col-desc")
                        if (sort_columns.indexOf("+" + column_name) !== -1)
                            col.addClass("col-ask")
                        if (sort_columns.indexOf("-" + column_name) !== -1)
                            col.addClass("col-desc")
                    }

                    data.sort(function (a, b) {
                        for (let index in sort_columns) {
                            let key = sort_columns[index].substr(1)
                            if (a[key] != b[key])
                                if (sort_columns[index][0] === "+")
                                    return a[key] > b[key] ? 1 : -1
                                else
                                    return a[key] < b[key] ? 1 : -1
                        }
                        return 0
                    })
                    sort(data)
                });
                sort(data)
            }
        }
        return ui
    }
);

