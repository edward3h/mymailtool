{
    mail: {
        store: {
            protocol: "imap"
        },
        user: "edward",
        host: "mail.example.com",
        port: 143,
        imap: {starttls: {enable: true}}
    },
    password: "xxxxxx",
    minage: "3 months",
    operations: 300


rule.1.source Inbox
rule.1.type move
rule.1.match *
rule.1.dest archive

rule.2.source archive
rule.2.type split
}