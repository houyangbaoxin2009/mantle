// FfmInteropProbe.java —— p.0.1.4 跨语言 tink v2 一帧互通探针（纯 JVM 驱动）
// ============================================================
// 职责（Roles）：纯 JVM、无任何仓库依赖的参考实现，双向验证与 tie 侧一帧互通：
//   1) 用自带的 tsha1f 快速模型参考实现核对五条权威金向量（fast: "", "123456789",
//      "abc", "a"*64 → 5juavlyl / 3Kz1piuc / 5FGIxu1J / 5juavlyl；strong digest
//      ("f","123456789",48) 前 64 hex → 260c7340...76bf）。
//   2) 方向一 Java→tie：Java 用参考实现编码 P1 快校验帧 → 让 tie 解析该帧（ok=1
//      integrity_ok=1），并核对 tie 对 P1 自己算出的 tsha1f(8) 与 Java 逐字节一致。
//   3) 方向二 tie→Java：请求 tie 编码 P2 → Java 用参考实现解析（integrity_ok=true、
//      载荷==P2、tsha1f(8) 与 tie 一致）。
// 退出码：全部通过 0，任一失败 1。摘要行打 DEBUG0（确定性、无墙钟）。
// EN: pure-JVM reference-implementation driver for the p.0.1.4 cross-language
// tink-v2 one-frame probe. Verifies the authoritative tsha1f golden vectors and
// both directions (Java→tie encode/parse; tie→Java parse). Exit 0 PASS / 1 FAIL.
//
// 用法（EN usage）：
//   javac FfmInteropProbe.java
//   java -DtieProbe=./tie_side/probe.exe FfmInteropProbe
//   （-DtieProbe 指向 tiecc 编译出的 probe.exe；缺省 ./tie_side/probe.exe）

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FfmInteropProbe {

    // ---- 帧常量（语言无关 ABI；逐式镜像 tie std/tink_v2 与 Subterra FrameConst，权威勿改）----
    static final int MAGIC_FIRST = 0x74;
    static final int MAGIC_SECOND = 0x6B;
    static final int FRAME_VERSION = 2;
    static final int FLAG_RESERVED_MASK = 0xE0;
    static final int INTEGRITY_FAST_BYTES = 8;
    static final int HEADER_BYTES = 10;

    // 探针载荷（EN: fixed probe payloads）
    static final String P1 = "interop-frame-01"; // 十六字节 ASCII
    static final String P2 = "interop-frame-02";

    static int FAIL_CTR = 0;

    public static void main(String[] args) throws Exception {
        String projExe = System.getProperty("tieProbe", "tie_side/probe.exe");

        // ---- 1) 金向量自证（EN: golden-vector self-assertion in the Java reference）----
        byte[] empty = new byte[0];
        byte[] nine = "123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] abc = "abc".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] a64 = new byte[64];
        Arrays.fill(a64, (byte) 'a');

        expectRaw(Tsha1f.tsha1f(empty, 8, 48).equals("5juavlyl"), "gv:empty");
        expectRaw(Tsha1f.tsha1f(nine, 8, 48).equals("3Kz1piuc"), "gv:nine");
        expectRaw(Tsha1f.tsha1f(abc, 8, 48).equals("5FGIxu1J"), "gv:abc");
        expectRaw(Tsha1f.tsha1f(a64, 8, 48).equals("5juavlyl"), "gv:a64");
        String strong = Tsha1f.digestHexF(nine, 48);
        String strong64 = strongLen(strong, 64);
        expectRaw(strong64.equals("260c7340bcc60eb82c727e6df6f9c7620d040c2d4837ee96dc333f1a16fd76bf"),
                "gv:strong");

        // ---- 2) 方向一：Java→tie（EN: Direction 1 Java→tie）----
        System.out.println("INTEROP:DIR1-START");
        byte[] p1 = P1.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        String fastJava = Tsha1f.tsha1f(p1, 8, 48);
        byte[] frameJava = encode(p1); // flags=0 → 快校验
        String frameHex = hex(frameJava);
        // Java 参考实现帧内校验段 == ascii(tsha1f(8))（自洽）
        byte[] integSlot = Arrays.copyOfRange(frameJava, frameJava.length - 8, frameJava.length);
        expectRaw(new String(integSlot, java.nio.charset.StandardCharsets.US_ASCII).equals(fastJava),
                "dir1:java_encode_intg");

        // tie 解析 Java 编码的帧（EN: tie parses the Java-encoded frame）
        List<String> paOut = runTie(projExe, "pa:" + frameHex);
        expectRaw(hasParsedOk(paOut, true, true), "dir1:tie_parse_java_frame");

        // tie 对 P1 的编码/tsha1f 与 Java 逐字节一致（一帧互通 #1，最强的字节级对等）
        List<String> enOut = runTie(projExe, "en:" + P1);
        String fastTie = valueOf(enOut, "FAST");
        String frameHexTie = valueOf(enOut, "FRAME");
        expectRaw(fastTie.equals(fastJava), "dir1:tie_tsha1f==java");
        expectRaw(frameHexTie.equals(frameHex), "dir1:tie_encode_byte_identical");
        System.out.println("DEB1:fastJava=" + fastJava + " fastTie=" + fastTie
                + " frameHex=" + frameHex + " frameHexTie=" + frameHexTie);

        // ---- 3) 方向二：tie→Java（EN: Direction 2 tie→Java）----
        System.out.println("INTEROP:DIR2-START");
        byte[] p2 = P2.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        List<String> en2 = runTie(projExe, "en:" + P2);
        String fastTie2 = valueOf(en2, "FAST");
        String frameHexTie2 = valueOf(en2, "FRAME");
        // Java 参考实现解析 tie 编码的帧（EN: Java parses the tie-encoded frame）
        byte[] decoded = parse(frameHexTie2);
        expectRaw(decoded != null, "dir2:parse_ok");
        if (decoded != null) {
            expectRaw(Arrays.equals(decoded, p2), "dir2:payload_eq");
        }
        String fastJava2 = Tsha1f.tsha1f(p2, 8, 48);
        // tsha1f(8) 两侧一致即为一帧互通（P2 的量级常数不预设，靠跨语言对等锚定；这是契约本体）
        expectRaw(fastJava2.equals(fastTie2) && fastJava2.length() == 8, "dir2:tsha1f_match");
        System.out.println("DEB2:fastTie2=" + fastTie2 + " fastJava2=" + fastJava2
                + " frameHexTie2=" + frameHexTie2);

        // ---- 摘要与退出码（EN: summary + exit code）----
        boolean allOk = FAIL_CTR == 0;
        String summary = "InteropProbe: " + (allOk ? "PASS" : "FAIL")
                + " dir1(parse+byte-identical)=" + (allOk ? "PASS" : "FAIL")
                + " dir2(parse+payload)=" + (allOk ? "PASS" : "FAIL")
                + " gvUsed=4 fast+1 strong";
        System.out.println("SUMMARY:" + summary);
        System.out.println("EXIT=" + (allOk ? "0" : "1"));
        System.exit(allOk ? 0 : 1);
    }

    // 取 hex 串前 n 字符（EN: first n chars of a hex string）
    static String strongLen(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    // 从 tie stdout 中按前缀取值（EN: extract value with given key prefix from tie stdout）
    static String valueOf(List<String> lines, String key) {
        String pref = key + ":";
        for (String l : lines) {
            if (l != null && l.startsWith(pref)) {
                return l.substring(pref.length());
            }
        }
        return null;
    }

    static boolean hasParsedOk(List<String> lines, boolean ok, boolean iok) {
        for (String l : lines) {
            if (l == null || !l.startsWith("PARSED:")) {
                continue;
            }
            String rest = l.substring("PARSED:".length());
            String[] kv = rest.split(" ");
            boolean okV = false, iokV = false;
            for (String pair : kv) {
                if (pair.startsWith("ok=") && pair.substring(3).equals(ok ? "1" : "0")) okV = true;
                if (pair.startsWith("iok=") && pair.substring(4).equals(iok ? "1" : "0")) iokV = true;
            }
            return okV && iokV;
        }
        return false;
    }

    // 运行 tie exe，喂若干命令行，返回全部 stdout 行（EN: run tie exe, feed lines, return stdout)
    static List<String> runTie(String exePath, String... inputs) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(exePath);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        for (String in : inputs) {
            p.getOutputStream().write((in + "\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        p.getOutputStream().close();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(),
                java.nio.charset.StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        p.waitFor();
        return lines;
    }

    static void expectRaw(boolean cond, String name) {
        if (cond) {
            System.out.println("CHECK:" + name + ":OK");
        } else {
            FAIL_CTR++;
            System.out.println("CHECK:" + name + ":FAIL");
        }
    }

    // ================= 快校验帧参考编解码（EN: fast-integrity frame reference codec）=================

    static byte[] encode(byte[] payload) {
        byte[] p = payload == null ? new byte[0] : payload;
        byte[] integ = fastIntegrity(p);
        byte[] out = new byte[HEADER_BYTES + p.length + integ.length];
        out[0] = (byte) MAGIC_FIRST;
        out[1] = (byte) MAGIC_SECOND;
        out[2] = (byte) FRAME_VERSION;
        out[3] = 0; // flags = 0（快校验）
        int len = p.length;
        out[4] = (byte) (len >>> 24);
        out[5] = (byte) (len >>> 16);
        out[6] = (byte) (len >>> 8);
        out[7] = (byte) len;
        out[8] = 0; // ext_len 高字节
        out[9] = 0; // ext_len 低字节
        System.arraycopy(p, 0, out, HEADER_BYTES, p.length);
        System.arraycopy(integ, 0, out, HEADER_BYTES + p.length, integ.length);
        return out;
    }

    static byte[] fastIntegrity(byte[] payload) {
        String s = Tsha1f.tsha1f(payload, 8, 48);
        return s.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    // hex 帧串 → 解析出 payload；完整性校验失败或格式坏返回 null（EN: parse fast frame hex → payload)
    static byte[] parse(String hexFrame) {
        byte[] buf = hexBytes(hexFrame);
        if (buf == null || buf.length < HEADER_BYTES) {
            return null;
        }
        boolean isV2 = (buf[0] & 0xFF) == MAGIC_FIRST && (buf[1] & 0xFF) == MAGIC_SECOND;
        if (!isV2) {
            return null; // 本探针只关心 v2
        }
        if ((buf[2] & 0xFF) != FRAME_VERSION) {
            return null;
        }
        int fl = buf[3] & 0xFF;
        if ((fl & FLAG_RESERVED_MASK) != 0) {
            return null;
        }
        int len = ((buf[4] & 0xFF) << 24) | ((buf[5] & 0xFF) << 16) | ((buf[6] & 0xFF) << 8) | (buf[7] & 0xFF);
        int el = ((buf[8] & 0xFF) << 8) | (buf[9] & 0xFF);
        int integLen = INTEGRITY_FAST_BYTES; // 本探针只编/解快校验帧（flags bit0=0）
        long total = HEADER_BYTES + (long) el + (long) len + integLen;
        if (total > buf.length) {
            return null;
        }
        int pstart = HEADER_BYTES + el;
        byte[] payload = Arrays.copyOfRange(buf, pstart, pstart + len);
        byte[] stored = Arrays.copyOfRange(buf, pstart + len, pstart + len + integLen);
        byte[] computed = fastIntegrity(payload);
        if (!Arrays.equals(computed, stored)) {
            return null;
        }
        return payload;
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append("0123456789abcdef".charAt((x >>> 4) & 15));
            sb.append("0123456789abcdef".charAt(x & 15));
        }
        return sb.toString();
    }

    static byte[] hexBytes(String h) {
        if (h.length() % 2 != 0) {
            return null;
        }
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ((hexVal(h.charAt(i * 2)) << 4) | hexVal(h.charAt(i * 2 + 1)));
        }
        return out;
    }

    static int hexVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return 0;
    }

    // ================= tsha1f 快速模型参考实现（EN: tsha1f fast-model reference implementation）=================
    // 逐式镜像 tie-main std/tsha1.tie（命名空间 tsha）的 f 模型通用路径与 Subterra
    // Tsha1f（只读参考）——本地最小自包含拷贝，用于本互通探针（不引入仓库依赖）。

    static final class Tsha1f {
        static final String B48_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKL";
        static final int[] VALID_N = {2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 69, 88, 92, 96, 128, 144};

        static final int[] IVF = parseHex32(
                "6f316818201ca2758e20cd45f4c7b538293d6b8de29e1c968f30cc67ad81cc6b"
                        + "0be4be38d3b994622e2ec7d603ac7d3079ce6a3441df35400f4659174d5f2f552"
                        + "6d77d9563cbfc8a547b09e5eb9ae1bc7ca9205c619064ba2cd8ab9e3a603f54"
                        + "3f1f16a3a9044df1956d21362cdfd96d1d518e07e49def028ce433befcd7e551");

        static final int[] RCONF = parseHex32(
                "7b440874437636f66dac446ced74969b9efe93b52a73975f59917ee17503e6d7e"
                        + "e9bfe4844bc2fe32327586f8c4dbff7c7a81e4d91daee663cab77ce54de351c");

        static int[] parseHex32(String hex) {
            int words = hex.length() / 8;
            int[] out = new int[words];
            for (int w = 0; w < words; w++) {
                int v = 0;
                for (int k = 0; k < 8; k++) {
                    v = (v << 4) | hexVal(hex.charAt(w * 8 + k));
                }
                out[w] = v;
            }
            return out;
        }

        static int rr32(int v, int n) {
            n &= 31;
            if (n == 0) return v;
            return (v >>> n) | (v << (32 - n));
        }

        static int rl32(int v, int n) {
            n &= 31;
            if (n == 0) return v;
            return (v << n) | (v >>> (32 - n));
        }

        static int rrp(int v, int r) {
            return rr32(v, r);
        }

        static int rrp16(int v, int r) {
            return rr32(v & 0xFFFF, r & 15);
        }

        static int fold16(int v) {
            return (v & 0xFFFF) ^ (v >>> 16);
        }

        static int rotlane(int x, int r, boolean half) {
            return half ? rrp16(fold16(x), r) : rrp(x, r);
        }

        static int[] tadd2(int ma, int na, int mb, int nb) {
            int nma = ma ^ 0xFFFFFFFF;
            int nna = na ^ 0xFFFFFFFF;
            int nmb = mb ^ 0xFFFFFFFF;
            int nnb = nb ^ 0xFFFFFFFF;
            int aP = ma & nna;
            int aN = ma & na;
            int bP = mb & nnb;
            int bN = mb & nb;
            int oPos = (nma & mb & nnb) | (ma & nna & nmb) | (aN & bN);
            int oNeg = (nma & mb & nb) | (ma & na & nmb) | (aP & bP);
            return new int[]{oPos | oNeg, oNeg};
        }

        static int[] tmul2(int ma, int na, int mb, int nb) {
            int amp = ma & mb;
            int no = (na ^ nb) & amp;
            return new int[]{amp, no};
        }

        static int quant3(int m0, int n0, int m1, int n1, int m2, int n2) {
            int a = m0 & (n0 ^ 0xFFFFFFFF);
            int b = m1 & (n1 ^ 0xFFFFFFFF);
            int c = m2 & (n2 ^ 0xFFFFFFFF);
            return (a & b) | (b & c) | (c & a);
        }

        static boolean isBits48(int n) {
            for (int v : VALID_N) {
                if (v == n) return true;
            }
            return false;
        }

        static boolean isBaseOk(int base) {
            return base == 2 || base == 3 || base == 8 || base == 16 || base == 48;
        }

        static int wordsFor(int n) {
            long w = (n * 1745300781475361L + 9999999999999999L) / 10000000000000000L;
            if (w < 1) return 1;
            return (int) w;
        }

        static int[] blockPlanesSkey(byte[] msg, int pos, int nbytes, boolean full, int[] Pw) {
            for (int kk = 0; kk < 16; kk++) {
                Pw[kk] = 0;
            }
            int m0v = 0, n0v = 0, m1v = 0, n1v = 0, skey = 0;
            for (int j = 0; j < 64; j++) {
                int b;
                int wv = j / 32;
                int bit = j % 32;
                if (full) {
                    b = msg[pos + j] & 0xFF;
                } else {
                    b = (pos + j < nbytes) ? (msg[pos + j] & 0xFF) : 0;
                }
                int bits3 = b & 7;
                int tf = (b >>> 6) & 3;
                int t = tf - 1;
                skey = (skey * 3 + (bits3 * 3 + (t + 1)));
                if (t != 0) {
                    if (t > 0) {
                        if (wv == 0) m0v |= (1 << bit);
                        else m1v |= (1 << bit);
                    } else {
                        if (wv == 0) n0v |= (1 << bit);
                        else n1v |= (1 << bit);
                    }
                }
                for (int biti = 0; biti < 8; biti++) {
                    int pi = biti * 2 + wv;
                    Pw[pi] |= (((b >>> biti) & 1) << bit);
                }
            }
            return new int[]{m0v, n0v, m1v, n1v, skey};
        }

        static void ringMix(int[] lanes, int start, int L, int r, int skey, int[] rcon,
                            boolean half, boolean inj,
                            int M0, int N0, int M1, int N1,
                            int mA, int mB, int rconIdx, int[] S) {
            if (L == 0) return;
            int rA = (r * 3 + (skey & 7)) & 31;
            int rB = (r * 7 + ((skey >>> 3) & 7)) & 31;
            int j2r = 2 % L;
            int j3r = 3 % L;
            for (int k = 0; k < L; k++) {
                int iw = start + k;
                int j2 = start + j2r;
                int j3 = start + j3r;
                int a = (rA + 5 * k) & 31;
                int b = (rB + 7 * k + 3) & 31;
                int[] s1 = tadd2(lanes[2 * iw], lanes[2 * iw + 1],
                        rotlane(lanes[2 * j2], a, half), rotlane(lanes[2 * j2 + 1], a, half));
                int[] s2 = tadd2(s1[0], s1[1],
                        rotlane(lanes[2 * j3], b, half), rotlane(lanes[2 * j3 + 1], b, half));
                S[2 * k] = s2[0];
                S[2 * k + 1] = s2[1];
                j2r++;
                if (j2r >= L) j2r = 0;
                j3r++;
                if (j3r >= L) j3r = 0;
            }
            for (int k = 0; k < L; k++) {
                lanes[2 * (start + k)] = S[2 * k];
                lanes[2 * (start + k) + 1] = S[2 * k + 1];
            }
            int k2r = 2 % L;
            int k1r = 1 % L;
            for (int k = 0; k < L; k++) {
                int[] pm = tmul2(S[2 * k], S[2 * k + 1], S[2 * k2r], S[2 * k2r + 1]);
                int i1 = start + k1r;
                lanes[2 * i1] ^= pm[0];
                lanes[2 * i1 + 1] ^= pm[1];
                k2r++;
                if (k2r >= L) k2r = 0;
                k1r++;
                if (k1r >= L) k1r = 0;
            }
            int maj;
            if (L >= 3) {
                maj = quant3(S[0], S[1], S[2], S[3], S[4], S[5]);
            } else if (L == 2) {
                maj = quant3(S[0], S[1], S[2], S[3], S[0], S[1]);
            } else {
                maj = quant3(S[0], S[1], S[0], S[1], S[0], S[1]);
            }
            int il = start + (L - 1);
            lanes[2 * il] ^= maj;
            lanes[2 * il + 1] ^= rotlane(maj, (rB + 7) & 31, half);
            if (inj) {
                int i0 = start;
                int[] s1 = tadd2(lanes[2 * i0], lanes[2 * i0 + 1],
                        rotlane(M0, mA, half), rotlane(N0, mA, half));
                lanes[2 * i0] = s1[0];
                lanes[2 * i0 + 1] = s1[1];
                if (L >= 2) {
                    int i1 = start + 1;
                    int[] s2 = tadd2(lanes[2 * i1], lanes[2 * i1 + 1],
                            rotlane(M1, (mA + 7) & 31, half), rotlane(N1, (mA + 7) & 31, half));
                    lanes[2 * i1] = s2[0];
                    lanes[2 * i1 + 1] = s2[1];
                }
            }
            lanes[2 * start] ^= rcon[rconIdx & 15];
            lanes[2 * (start + L - 1) + 1] ^= rotlane(rcon[(rconIdx + 1) & 15], (r * 5) & 31, half);
        }

        static void compressF(int[] h, byte[] msg, int pos, int nbytes,
                              int tLo, int tHi, boolean last,
                              int[] lanes, int[] Pw, int[] S) {
            int W = h.length;
            boolean full = (pos + 64 <= nbytes);
            int[] msk = blockPlanesSkey(msg, pos, nbytes, full, Pw);
            int M0 = msk[0], N0 = msk[1], M1 = msk[2], N1 = msk[3], skey = msk[4];
            int mPlan = 0xFFFFFFFF;

            for (int i = 0; i < W; i++) {
                lanes[2 * i] = h[i];
                lanes[2 * i + 1] = rotlane(h[(i + W / 2) % W], 7, false);
            }
            for (int i = 0; i < W; i++) {
                lanes[2 * i] ^= (IVF[i % 8] & mPlan);
                lanes[2 * i + 1] ^= (IVF[(i + 4) % 8] & mPlan);
            }
            lanes[1] ^= (tLo & mPlan);
            int hi = 2 * (W / 2) + 1;
            lanes[hi] ^= (tHi & mPlan);
            if (last) {
                int lk = 2 * ((W / 2) % W) + 1;
                lanes[lk] ^= 0xFFFFFFFF;
            }

            // absorb
            for (int w = 0; w < 16; w++) {
                if (Pw[w] == 0) continue;
                int a = (w * 5 + ((skey >>> (w & 7)) & 7)) & 31;
                int an = w % W;
                int i2 = 2 * an;
                int v = rrp(Pw[w], a);
                int v2 = rrp(Pw[w], ((a + 17) & 31));
                int[] s = tadd2(lanes[i2], lanes[i2 + 1], v, v2);
                lanes[i2] = s[0];
                lanes[i2 + 1] = s[1];
            }

            int tCount;
            int[] ts = new int[4];
            int[] tl = new int[4];
            int[] tk = new int[4];
            if (W >= 16) {
                int la = (W + 1) / 2;
                ts[0] = 0; tl[0] = la; tk[0] = 0;
                ts[1] = la; tl[1] = W - la; tk[1] = 0;
                tCount = 2;
            } else if (W >= 8) {
                int la3 = (W + 1) / 2;
                ts[0] = 0; tl[0] = la3; tk[0] = 0;
                ts[1] = la3; tl[1] = W - la3; tk[1] = 0;
                tCount = 2;
            } else {
                ts[0] = 0; tl[0] = W; tk[0] = 1;
                tCount = 1;
            }
            int R = 12;
            for (int r = 0; r < R; r++) {
                int mA = (r * 5 + ((skey >>> 6) & 7)) & 31;
                int mB = (r * 11 + ((skey >>> 9) & 7)) & 31;
                int mC = (r * 7 + ((skey >>> 12) & 7)) & 31;
                int mD = (r * 13 + ((skey >>> 15) & 7)) & 31;
                int dualSeen = 0;
                for (int tt = 0; tt < tCount; tt++) {
                    int kind = tk[tt];
                    if (kind == 0) {
                        int injmA = mA;
                        int injmB = mB;
                        int rc = r;
                        if (dualSeen > 0) {
                            injmA = mC;
                            injmB = mD;
                            rc = (r + 8) & 15;
                        }
                        ringMix(lanes, ts[tt], tl[tt], r, skey, RCONF, false, true,
                                M0, N0, M1, N1, injmA, injmB, rc, S);
                        dualSeen = dualSeen + 1;
                    } else {
                        ringMix(lanes, ts[tt], tl[tt], r, skey, RCONF, false, true,
                                M0, N0, M1, N1, mA, mB, r, S);
                    }
                }
            }

            for (int i = 0; i < W; i++) {
                h[i] ^= lanes[2 * i] ^ rotlane(lanes[2 * i + 1], (i * 3) & 31, false);
            }
            finSynth(h, lanes, Pw, S);
        }

        static void finSynth(int[] h, int[] lanes, int[] Pw, int[] S) {
            int W = h.length;
            if (W == 1) return;
            for (int i = 0; i < W; i++) {
                lanes[2 * i] = h[i];
                lanes[2 * i + 1] = rotlane(h[(i + 1) % W], 7, false);
            }
            for (int z = 0; z < 16; z++) {
                Pw[z] = 0;
            }
            for (int sf = 0; sf < 4; sf++) {
                ringMix(lanes, 0, W, sf, 0x13579BDF, Pw, false, false,
                        0, 0, 0, 0, 0, 0, sf, S);
            }
            for (int i = 0; i < W; i++) {
                h[i] = lanes[2 * i] ^ rotlane(lanes[2 * i + 1], (i * 5) & 31, false);
            }
        }

        static String digestHexF(byte[] msg, int n) {
            if (!isBits48(n)) return "";
            int nbytes = msg.length;
            int W = wordsFor(n);
            int[] h = new int[W];
            for (int i = 0; i < W; i++) {
                h[i] = IVF[i];
            }
            h[0] ^= 0x01010000 ^ 32;
            int tLo = 0, tHi = 0, pos = 0;
            int[] lanes = new int[2 * W + 2];
            int[] Pw = new int[16];
            int[] S = new int[2 * W + 2];
            while (pos + 64 < nbytes) {
                long lo64 = tLo & 0xFFFFFFFFL;
                tHi = (int) (tHi + (lo64 + 64) >>> 32);
                tLo = (int) ((lo64 + 64) & 0xFFFFFFFFL);
                compressF(h, msg, pos, nbytes, tLo, tHi, false, lanes, Pw, S);
                pos = pos + 64;
            }
            int rem = nbytes - pos;
            long lo64b = tLo & 0xFFFFFFFFL;
            tHi = (int) (tHi + (lo64b + rem) >>> 32);
            tLo = (int) ((lo64b + rem) & 0xFFFFFFFFL);
            compressF(h, msg, pos, nbytes, tLo, tHi, true, lanes, Pw, S);
            StringBuilder out = new StringBuilder(8 * W);
            for (int i = 0; i < W; i++) {
                String hx = Integer.toHexString(h[i]);
                for (int p = hx.length(); p < 8; p++) {
                    out.append('0');
                }
                out.append(hx);
            }
            return out.toString();
        }

        static String b48Min(String hx) {
            if (hx.isEmpty()) return "";
            int L = hx.length() / 2;
            int[] b = new int[L];
            for (int i = 0; i < L; i++) {
                b[i] = hexByte(hx, i * 2);
            }
            List<Integer> digits = new ArrayList<>();
            int pos = 0;
            while (pos < L) {
                int rem = 0;
                for (int i = 0; i < L; i++) {
                    int cur = rem * 256 + b[i];
                    b[i] = cur / 48;
                    rem = cur % 48;
                }
                while (pos < L && b[pos] == 0) {
                    pos = pos + 1;
                }
                digits.add(rem);
            }
            StringBuilder out = new StringBuilder(digits.size());
            for (int i = digits.size() - 1; i >= 0; i--) {
                out.append(B48_ALPHABET.charAt(digits.get(i)));
            }
            return out.length() == 0 ? "0" : out.toString();
        }

        static int hexByte(String hex, int i) {
            return hexVal(hex.charAt(i)) * 16 + hexVal(hex.charAt(i + 1));
        }

        public static String tsha1f(byte[] msg, int n, int base) {
            if (!isBits48(n)) return "";
            int b = base <= 0 ? 48 : base;
            if (!isBaseOk(b)) return "";
            byte[] m = msg == null ? new byte[0] : msg;
            String hexstr = digestHexF(m, n);
            if (hexstr.isEmpty()) return "";
            int nbytes = m.length;
            if (b == 48) {
                String full = b48Min(hexstr);
                int fl = full.length();
                if (fl < n) {
                    StringBuilder pad = new StringBuilder();
                    for (int k = 0; k < n - fl; k++) {
                        pad.append('0');
                    }
                    return pad + full;
                }
                return full.substring(0, n);
            }
            int b0 = (n * 698121) / 1000000;
            if (hexstr.length() / 2 < b0) return "";
            String bb48 = hexstr.substring(0, b0 * 2);
            if (b == 16) return bb48;
            int m2 = baseCharsCount(b0, b);
            return padRadix(bb48, b, m2);
        }

        static int baseCharsCount(int b, int base) {
            if (base == 2) return b * 8;
            if (base == 3) return (b * 504744 + 99999) / 100000;
            if (base == 8) return (b * 8 + 2) / 3;
            if (base == 16) return b * 2;
            return (b * 1432407) / 1000000 + 1;
        }

        static String padRadix(String hex, int base, int m) {
            int L = hex.length() / 2;
            int[] b = new int[L];
            for (int i = 0; i < L; i++) {
                b[i] = hexByte(hex, i * 2);
            }
            List<Integer> digs = new ArrayList<>();
            while (true) {
                int rem = 0;
                int[] nq = new int[b.length];
                boolean allz = true;
                for (int j = 0; j < b.length; j++) {
                    int cur = rem * 256 + b[j];
                    int q = cur / base;
                    int r = cur % base;
                    if (q != 0) allz = false;
                    nq[j] = q;
                    rem = r;
                }
                digs.add(rem);
                b = nq;
                if (allz) break;
            }
            StringBuilder out = new StringBuilder();
            for (int k = digs.size() - 1; k >= 0; k--) {
                int d = digs.get(k);
                out.append(d < 10 ? (char) ('0' + d) : (char) ('a' + (d - 10)));
            }
            while (out.length() < m) {
                out.insert(0, '0');
            }
            return out.toString();
        }
    }
}