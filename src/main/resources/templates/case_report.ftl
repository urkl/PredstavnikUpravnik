<!DOCTYPE html>
<html lang="sl">
<head>
    <meta charset="UTF-8"/>
    <style>
        @import url("https://fonts.googleapis.com/css2?family=Ubuntu:ital,wght@0,300;0,400;0,500;0,700;1,300;1,400;1,500;1,700&amp;display=swap");

        body { font-family: 'Ubuntu', sans-serif; font-size: 10pt; color: #333; }
        .header { text-align: center; border-bottom: 2px solid #4a5568; padding-bottom: 10px; margin-bottom: 25px; }
        .header h1 { margin: 0; color: #2d3748; font-size: 22pt; }
        .header .app-name { font-size: 12pt; color: #718096; }
        h2 { font-size: 14pt; color: #2d3748; border-bottom: 1px solid #e2e8f0; padding-bottom: 5px; margin-top: 25px; margin-bottom: 15px; }
        .details-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        .details-table td { padding: 9px 5px; border: 1px solid #e2e8f0; }
        .details-table .label { font-weight: bold; background-color: #f7fafc; width: 150px; }
        .description { background-color: #f7fafc; padding: 15px; border-radius: 5px; border: 1px solid #e2e8f0; white-space: pre-wrap; line-height: 1.5; }
        .subtasks ul { list-style-type: none; padding-left: 0; }
        .subtasks li { margin-bottom: 6px; font-size: 10pt; }
        .subtask-completed { text-decoration: line-through; color: #a0aec0; }
        .comment { border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-bottom: 10px; background-color: #ffffff; }
        .comment-header { font-size: 9pt; color: #718096; margin-bottom: 5px; }
        .comment-header .author { font-weight: bold; color: #4a5568; }
        .comment-content { margin: 0; line-height: 1.4; }
        .no-content { color: #a0aec0; font-style: italic; }

        /* --- NOV STIL ZA NOGO --- */
        .footer {
            text-align: center;
            margin-top: 40px;
            padding-top: 15px;
            border-top: 1px solid #e2e8f0;
            font-size: 9pt;
            color: #718096;
        }
    </style>
</head>
<body>

<div class="header">
    <div class="app-name">BlokApp Poročilo o Zadevi</div>
    <h1>${case.title! 'Brez naslova'}</h1>
</div>

<h2>Osnovni podatki</h2>
<table class="details-table">
    <tr>
        <td class="label">Status:</td>
        <td>${status! 'Ni določen'}</td>
        <td class="label">Prijavitelj:</td>
        <td><#if case.author??>${case.author.name! 'Neznan'}<#else>Neznan</#if></td>
    </tr>
    <tr>
        <td class="label">Datum prijave:</td>
        <td>${createdDateFormatted! 'Ni zabeležen'}</td>
        <td class="label">Zadnja sprememba:</td>
        <td>${lastModifiedDateFormatted! 'Ni zabeležena'}</td>
    </tr>
    <tr>
        <td class="label">Časovni okvir del:</td>
        <td colspan="3">
            <#if startDateFormatted?? || endDateFormatted??>
                <#if startDateFormatted??>Od ${startDateFormatted}</#if> <#if endDateFormatted??>do ${endDateFormatted}</#if>
            <#else>
                Ni določen
            </#if>
        </td>
    </tr>
    <tr>
        <td class="label">Objekti:</td>
        <td colspan="3">${buildings! 'Niso določeni'}</td>
    </tr>
</table>

<h2>Opis zadeve</h2>
<div class="description">
    ${case.description! 'Opis ni na voljo.'}
</div>

<#if case.subtasks?has_content>
    <h2>Podnaloge</h2>
    <div class="subtasks">
        <ul>
            <#list case.subtasks as subtask>
                <li class="<#if subtask.completed>subtask-completed</#if>">
                    &#974<#if subtask.completed>6<#else>4</#if>; ${subtask.task}
                </li>
            </#list>
        </ul>
    </div>
<#else>
    <h2>Podnaloge</h2>
    <p class="no-content">Ni dodanih podnalog.</p>
</#if>

<#if comments?has_content>
    <h2>Komentarji</h2>
    <div class="comments">
        <#list comments as comment>
            <div class="comment">
                <div class="comment-header">
                    <span class="author">${comment.author! 'Neznan avtor'}</span>
                    <span class="timestamp"> - ${comment.timestamp! ''}</span>
                </div>
                <p class="comment-content">${comment.content! ''}</p>
            </div>
        </#list>
    </div>
<#else>
    <h2>Komentarji</h2>
    <p class="no-content">Ni komentarjev.</p>
</#if>

<div class="footer">
    Urosk.NET - Uroš Kristan
</div>

</body>
</html>