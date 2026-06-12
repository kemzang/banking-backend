--
-- PostgreSQL database dump
--

-- Dumped from database version 16.0
-- Dumped by pg_dump version 16.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transactions (
    id bigint NOT NULL,
    reference character varying(32) NOT NULL,
    type character varying(20) NOT NULL,
    montant numeric(19,2) NOT NULL,
    devise character varying(3) NOT NULL,
    compte_source_id bigint,
    compte_dest_id bigint,
    operateur_source_id bigint,
    operateur_dest_id bigint,
    commission numeric(19,2) DEFAULT 0.00 NOT NULL,
    statut character varying(20) NOT NULL,
    motif character varying(500),
    date_operation timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT chk_depot_compte CHECK ((((type)::text <> 'DEPOT'::text) OR ((compte_source_id IS NULL) AND (compte_dest_id IS NOT NULL)))),
    CONSTRAINT chk_retrait_compte CHECK ((((type)::text <> 'RETRAIT'::text) OR ((compte_source_id IS NOT NULL) AND (compte_dest_id IS NULL)))),
    CONSTRAINT chk_transaction_commission_positive CHECK ((commission >= (0)::numeric)),
    CONSTRAINT chk_transaction_devise_not_empty CHECK ((length(TRIM(BOTH FROM devise)) > 0)),
    CONSTRAINT chk_transaction_montant_positive CHECK ((montant > (0)::numeric)),
    CONSTRAINT chk_transaction_statut CHECK (((statut)::text = ANY (ARRAY[('INITIEE'::character varying)::text, ('VALIDEE'::character varying)::text, ('REJETEE'::character varying)::text]))),
    CONSTRAINT chk_transaction_type CHECK (((type)::text = ANY (ARRAY[('DEPOT'::character varying)::text, ('RETRAIT'::character varying)::text, ('TRANSFERT'::character varying)::text]))),
    CONSTRAINT chk_transfert_compte CHECK ((((type)::text <> 'TRANSFERT'::text) OR ((compte_source_id IS NOT NULL) AND (compte_dest_id IS NOT NULL) AND (compte_source_id <> compte_dest_id))))
);


--
-- Name: transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transactions_id_seq OWNED BY public.transactions.id;


--
-- Name: transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions ALTER COLUMN id SET DEFAULT nextval('public.transactions_id_seq'::regclass);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: transactions transactions_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_reference_key UNIQUE (reference);


--
-- Name: idx_transactions_compte_dest; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transactions_compte_dest ON public.transactions USING btree (compte_dest_id);


--
-- Name: idx_transactions_compte_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transactions_compte_source ON public.transactions USING btree (compte_source_id);


--
-- Name: idx_transactions_date_operation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transactions_date_operation ON public.transactions USING btree (date_operation);


--
-- Name: idx_transactions_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transactions_reference ON public.transactions USING btree (reference);


--
-- Name: idx_transactions_statut; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transactions_statut ON public.transactions USING btree (statut);


--
-- Name: idx_transactions_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transactions_type ON public.transactions USING btree (type);


--
-- Name: transactions trg_transactions_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_transactions_updated_at BEFORE UPDATE ON public.transactions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- PostgreSQL database dump complete
--

