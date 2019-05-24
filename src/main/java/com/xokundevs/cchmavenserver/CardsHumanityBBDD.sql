drop database if exists CardsHumanity;

create database CardsHumanity;
use CardsHumanity;

drop user if exists cchAdmin;
create user cchAdmin identified by 'rbacrm90';
grant all privileges on CardsHumanity.* to cchAdmin;
flush privileges;

create table Usuario(
	emailUsuario varchar(50),
	nombreUsuario varchar(15),
    contrasenya varchar(100),
    partidasGanadas int,
    imagenPerfil varchar(200),
    primary key(emailUsuario)
);

create table Baraja(
    emailUsuario varchar(50),
    nombreBaraja varchar(30),
    idioma varchar(10),
    primary key(emailUsuario, nombreBaraja)
);

create table Carta(
	idCarta int,
    emailUsuario varchar(50),
    nombreBaraja varchar(30),
    texto varchar(200),
    primary key(idCarta, emailUsuario, nombreBaraja)
);

create table CartaNegra(
	idCarta int,
    emailUsuario varchar(50),
    nombreBaraja varchar(30),
    numeroEspacios int,
    primary key(idCarta, emailUsuario, nombreBaraja)
);

create table CartaBlanca(
	idCarta int,
    emailUsuario varchar(50),
    nombreBaraja varchar(30),
    primary key(idCarta, emailUsuario, nombreBaraja)
);

alter table Baraja add constraint fk_us_ba foreign key(emailUsuario) references Usuario(emailUsuario);
alter table Carta add constraint fk_ba_ca foreign key(emailUsuario, nombreBaraja) references Baraja(emailUsuario, nombreBaraja);
alter table CartaNegra add constraint fk_ca_caN foreign key(idCarta, emailUsuario, nombreBaraja) references Carta(idCarta, emailUsuario, nombreBaraja);
alter table CartaBlanca add constraint fk_ca_caB foreign key(idCarta, emailUsuario, nombreBaraja) references Carta(idCarta, emailUsuario, nombreBaraja);

insert into Usuario values('default', 'default', 'default', 0, '');
insert into Usuario values('123456@chokundevs.com', 'chokundeveloper', '7c4a8d09ca3762af61e59520943dc26494f8941b', 0, '');

select * from Usuario;